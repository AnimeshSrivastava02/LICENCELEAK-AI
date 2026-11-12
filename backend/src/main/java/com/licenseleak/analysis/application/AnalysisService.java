package com.licenseleak.analysis.application;

import com.licenseleak.analysis.domain.AnalysisOutcome;
import com.licenseleak.analysis.domain.AnalysisRunEntity;
import com.licenseleak.analysis.infrastructure.AnalysisRunRepository;
import com.licenseleak.analysis.api.AnalysisResponse;
import com.licenseleak.audit.application.AuditService;
import com.licenseleak.cases.application.CaseNotFoundException;
import com.licenseleak.cases.domain.DemoCaseEntity;
import com.licenseleak.cases.domain.LicenseContractEntity;
import com.licenseleak.cases.domain.UsageEvidenceEntity;
import com.licenseleak.cases.infrastructure.DemoCaseRepository;
import com.licenseleak.cases.infrastructure.LicenseContractRepository;
import com.licenseleak.cases.infrastructure.UsageEvidenceRepository;
import com.licenseleak.fees.application.DailyFeeCalculator;
import com.licenseleak.fees.api.FeeQuote;
import com.licenseleak.fees.domain.FeeQuoteEntity;
import com.licenseleak.fees.domain.FeeQuoteItemEntity;
import com.licenseleak.fees.infrastructure.FeeQuoteRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisService {

    private static final Pattern CLAUSE_ID = Pattern.compile(
            "\\bclause(?:\\s+|-)([a-zA-Z0-9]+(?:-[a-zA-Z0-9]+)*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern USAGE_CONDITION_SECTION = Pattern.compile(
            "\\busage\\s+condition\\s*:", Pattern.CASE_INSENSITIVE);

    private final AnalysisProvider provider;
    private final AnalysisRunRepository analysisRepository;
    private final DemoCaseRepository caseRepository;
    private final LicenseContractRepository contractRepository;
    private final UsageEvidenceRepository evidenceRepository;
    private final FeeQuoteRepository quoteRepository;
    private final DailyFeeCalculator feeCalculator;
    private final AuditService auditService;
    private final TransactionTemplate transactionTemplate;

    public AnalysisService(AnalysisProvider provider, AnalysisRunRepository analysisRepository,
            DemoCaseRepository caseRepository, LicenseContractRepository contractRepository,
            UsageEvidenceRepository evidenceRepository, FeeQuoteRepository quoteRepository,
            DailyFeeCalculator feeCalculator, AuditService auditService,
            PlatformTransactionManager transactionManager) {
        this.provider = provider;
        this.analysisRepository = analysisRepository;
        this.caseRepository = caseRepository;
        this.contractRepository = contractRepository;
        this.evidenceRepository = evidenceRepository;
        this.quoteRepository = quoteRepository;
        this.feeCalculator = feeCalculator;
        this.auditService = auditService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public AnalysisResponse analyze(String caseId) {
        DemoCaseEntity demoCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new CaseNotFoundException(caseId));
        LicenseContractEntity contract = contractRepository.findByCaseId(caseId)
                .orElseThrow(() -> new IllegalStateException("Case is missing contract metadata"));
        List<UsageEvidenceEntity> evidence = evidenceRepository.findAllByCaseIdOrderByIdAsc(caseId);
        AnalysisInput input = new AnalysisInput(caseId, demoCase.getTitle(), demoCase.getScenarioType(),
                contract.getVersion(), contract.getTitle(), contract.getSummary(), contract.getContractValidThrough(),
                evidence.stream().map(item -> new AnalysisInput.Evidence(item.getId(), item.getType(), item.getLabel(),
                        item.getAdReceivedOn(), item.isOutOfContract())).toList());

        AnalysisDraft draft = normalize(provider.analyze(input), input, contract.getSummary());
        return transactionTemplate.execute(status -> persistAnalysis(caseId, contract, evidence, draft));
    }

    private AnalysisResponse persistAnalysis(String caseId, LicenseContractEntity contract,
            List<UsageEvidenceEntity> evidence, AnalysisDraft draft) {
        String analysisId = UUID.randomUUID().toString();
        int version = analysisRepository.countByCaseId(caseId) + 1;
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        AnalysisRunEntity analysis = analysisRepository.save(new AnalysisRunEntity(
                analysisId, caseId, version, draft.outcome(), draft.summary(), draft.source(), draft.modelName(),
                contract.getVersion(), now, draft.clauseReferences(), draft.evidenceReferences(), draft.uncertainties()));

        FeeQuoteEntity quote = createQuote(analysis, contract, evidence, draft.outcome(), now);
        if (quote != null) {
            quote = quoteRepository.save(quote);
        }
        auditService.record(caseId, "ANALYSIS_CREATED", analysisId,
                java.util.Map.of("version", version, "outcome", draft.outcome().name(), "source", draft.source()));
        return AnalysisResponse.from(analysis, quote);
    }

    @Transactional(readOnly = true)
    public AnalysisResponse latest(String caseId) {
        if (!caseRepository.existsById(caseId)) {
            throw new CaseNotFoundException(caseId);
        }
        AnalysisRunEntity analysis = analysisRepository.findFirstByCaseIdOrderByVersionDesc(caseId)
                .orElseThrow(() -> new AnalysisNotFoundException(caseId));
        return AnalysisResponse.from(analysis, quoteRepository.findByAnalysisId(analysis.getId()).orElse(null));
    }

    private AnalysisDraft normalize(AnalysisDraft draft, AnalysisInput input, String contractText) {
        if (draft == null || draft.outcome() == null || draft.summary() == null || draft.summary().isBlank()
                || draft.clauseReferences() == null || draft.evidenceReferences() == null || draft.uncertainties() == null) {
            return ambiguous("The analysis response was incomplete and needs human review.",
                    List.of("The analysis provider returned missing or invalid fields."), input.evidence());
        }

        Set<String> evidenceIds = new HashSet<>();
        input.evidence().forEach(item -> evidenceIds.add(item.evidenceId()));
        Set<String> allowedClauses = new HashSet<>();
        Matcher matcher = CLAUSE_ID.matcher(contractText);
        while (matcher.find()) {
            allowedClauses.add("clause-" + matcher.group(1).toLowerCase(Locale.ROOT));
        }
        if (USAGE_CONDITION_SECTION.matcher(contractText).find()) {
            allowedClauses.add("usage-condition");
        }
        boolean invalidReference = draft.evidenceReferences().stream().anyMatch(id -> !evidenceIds.contains(id))
                || draft.clauseReferences().stream().anyMatch(id -> !allowedClauses.contains(id.toLowerCase()));
        if (invalidReference) {
            return ambiguous("The analysis referenced material that is not present in this case.",
                    List.of("The returned references could not be validated against the case records."), List.of());
        }
        if (draft.outcome() != AnalysisOutcome.AMBIGUOUS
                && (draft.clauseReferences().isEmpty() || draft.evidenceReferences().isEmpty())) {
            return ambiguous("The analysis did not provide enough source references to support its outcome.",
                    List.of("A non-ambiguous result must cite at least one contract clause and one evidence item."),
                    input.evidence());
        }

        boolean missingDatesForOutOfContractAd = input.evidence().stream()
                .filter(AnalysisInput.Evidence::outOfContract)
                .anyMatch(item -> input.contractValidThrough() == null || item.receivedOn() == null);
        if (missingDatesForOutOfContractAd && draft.outcome() != AnalysisOutcome.AMBIGUOUS) {
            List<String> uncertainties = new ArrayList<>(draft.uncertainties());
            uncertainties.add("Contract end date or ad receipt date is missing; human review is required.");
            return new AnalysisDraft(AnalysisOutcome.AMBIGUOUS,
                    "The case has an out-of-contract ad record, but the dates needed to calculate the period are incomplete.",
                    draft.clauseReferences(), draft.evidenceReferences(), uncertainties, draft.source(), draft.modelName());
        }
        return new AnalysisDraft(draft.outcome(), draft.summary(), draft.clauseReferences(),
                draft.evidenceReferences(), draft.uncertainties(),
                draft.source() == null ? "VALIDATION" : draft.source(), draft.modelName());
    }

    private AnalysisDraft ambiguous(String summary, List<String> uncertainties, List<AnalysisInput.Evidence> evidence) {
        return new AnalysisDraft(AnalysisOutcome.AMBIGUOUS, summary, List.of(),
                evidence.stream().map(AnalysisInput.Evidence::evidenceId).toList(), uncertainties, "VALIDATION", null);
    }

    private FeeQuoteEntity createQuote(AnalysisRunEntity analysis, LicenseContractEntity contract,
            List<UsageEvidenceEntity> evidence, AnalysisOutcome outcome, OffsetDateTime now) {
        if (outcome != AnalysisOutcome.POSSIBLE_MISMATCH) {
            return null;
        }
        List<UsageEvidenceEntity> outOfContract = evidence.stream().filter(UsageEvidenceEntity::isOutOfContract).toList();
        if (outOfContract.isEmpty()) {
            return null;
        }

        List<FeeQuoteItemEntity> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        int totalDays = 0;
        boolean actionable = true;
        for (UsageEvidenceEntity item : outOfContract) {
            if (contract.getContractValidThrough() == null || item.getAdReceivedOn() == null) {
                items.add(new FeeQuoteItemEntity(item.getId(), contract.getContractValidThrough(),
                        item.getAdReceivedOn(), 0, null, false));
                actionable = false;
                continue;
            }
            FeeQuote line = feeCalculator.calculate(contract.getContractValidThrough(), item.getAdReceivedOn());
            int days = Math.toIntExact(line.billableDays());
            totalDays = Math.addExact(totalDays, days);
            total = total.add(line.amount());
            items.add(new FeeQuoteItemEntity(item.getId(), line.contractValidThrough(), line.adReceivedOn(),
                    days, line.amount(), true));
        }
        if (total.signum() <= 0) {
            actionable = false;
        }
        return new FeeQuoteEntity(UUID.randomUUID().toString(), analysis.getId(), actionable ? total : null,
                feeCalculator.getCurrency(), feeCalculator.getDailyRate(),
                totalDays, actionable, "DAILY_RATE_PER_BILLABLE_DAY", now, items);
    }
}
