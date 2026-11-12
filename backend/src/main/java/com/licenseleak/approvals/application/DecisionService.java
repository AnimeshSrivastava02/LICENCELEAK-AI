package com.licenseleak.approvals.application;

import com.licenseleak.analysis.domain.AnalysisRunEntity;
import com.licenseleak.analysis.infrastructure.AnalysisRunRepository;
import com.licenseleak.approvals.api.DecisionRequest;
import com.licenseleak.approvals.api.DecisionResponse;
import com.licenseleak.approvals.domain.CreatorDecisionEntity;
import com.licenseleak.approvals.domain.DecisionType;
import com.licenseleak.approvals.infrastructure.CreatorDecisionRepository;
import com.licenseleak.audit.application.AuditService;
import com.licenseleak.cases.application.CaseNotFoundException;
import com.licenseleak.cases.infrastructure.DemoCaseRepository;
import com.licenseleak.common.errors.ConflictException;
import com.licenseleak.fees.domain.FeeQuoteEntity;
import com.licenseleak.fees.infrastructure.FeeQuoteRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DecisionService {

    private final DemoCaseRepository caseRepository;
    private final AnalysisRunRepository analysisRepository;
    private final FeeQuoteRepository quoteRepository;
    private final CreatorDecisionRepository decisionRepository;
    private final AuditService auditService;

    public DecisionService(DemoCaseRepository caseRepository, AnalysisRunRepository analysisRepository,
            FeeQuoteRepository quoteRepository, CreatorDecisionRepository decisionRepository, AuditService auditService) {
        this.caseRepository = caseRepository;
        this.analysisRepository = analysisRepository;
        this.quoteRepository = quoteRepository;
        this.decisionRepository = decisionRepository;
        this.auditService = auditService;
    }

    @Transactional
    public DecisionResponse decide(String caseId, DecisionRequest request) {
        if (!caseRepository.existsById(caseId)) {
            throw new CaseNotFoundException(caseId);
        }
        AnalysisRunEntity latest = analysisRepository.findFirstByCaseIdOrderByVersionDesc(caseId)
                .orElseThrow(() -> new ConflictException("ANALYSIS_REQUIRED", "Run an analysis before recording a decision."));
        if (!latest.getId().equals(request.analysisId())) {
            throw new ConflictException("STALE_ANALYSIS", "The decision must reference the latest analysis version." );
        }

        FeeQuoteEntity quote = null;
        if (request.feeQuoteId() != null && !request.feeQuoteId().isBlank()) {
            quote = quoteRepository.findByIdAndAnalysisId(request.feeQuoteId(), latest.getId())
                    .orElseThrow(() -> new ConflictException("INVALID_FEE_QUOTE", "The fee quote does not belong to this analysis."));
        }
        if (request.decision() == DecisionType.APPROVE && quote != null && !quote.isActionable()) {
            throw new ConflictException("FEE_QUOTE_NOT_ACTIONABLE", "This fee quote is incomplete and cannot be approved for invoicing." );
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        CreatorDecisionEntity entity = decisionRepository.save(new CreatorDecisionEntity(
                UUID.randomUUID().toString(), caseId, latest.getId(), quote == null ? null : quote.getId(),
                request.decision(), now));
        auditService.record(caseId, "DECISION_RECORDED", entity.getId(),
                Map.of("decision", entity.getDecision().name(), "analysisId", latest.getId(),
                        "feeQuoteId", quote == null ? "" : quote.getId()));
        return DecisionResponse.from(entity);
    }

    @Transactional(readOnly = true)
    public DecisionResponse latest(String caseId) {
        if (!caseRepository.existsById(caseId)) {
            throw new CaseNotFoundException(caseId);
        }
        return decisionRepository.findTopByCaseIdOrderByDecidedAtDesc(caseId)
                .map(DecisionResponse::from)
                .orElseThrow(() -> new DecisionNotFoundException(caseId));
    }
}
