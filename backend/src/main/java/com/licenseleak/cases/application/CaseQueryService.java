package com.licenseleak.cases.application;

import com.licenseleak.cases.api.CaseDetailResponse;
import com.licenseleak.cases.api.CaseDetailResponse.ContractSummary;
import com.licenseleak.cases.api.CaseDetailResponse.EvidenceSummary;
import com.licenseleak.cases.api.CaseSummaryResponse;
import com.licenseleak.cases.domain.DemoCaseEntity;
import com.licenseleak.cases.domain.LicenseContractEntity;
import com.licenseleak.cases.infrastructure.DemoCaseRepository;
import com.licenseleak.cases.infrastructure.LicenseContractRepository;
import com.licenseleak.cases.infrastructure.UsageEvidenceRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CaseQueryService {

    private final DemoCaseRepository caseRepository;
    private final LicenseContractRepository contractRepository;
    private final UsageEvidenceRepository evidenceRepository;

    public CaseQueryService(
            DemoCaseRepository caseRepository,
            LicenseContractRepository contractRepository,
            UsageEvidenceRepository evidenceRepository) {
        this.caseRepository = caseRepository;
        this.contractRepository = contractRepository;
        this.evidenceRepository = evidenceRepository;
    }

    public List<CaseSummaryResponse> listCases() {
        return caseRepository.findAllByOrderByTitleAsc().stream()
                .map(this::toSummary)
                .toList();
    }

    public CaseDetailResponse getCase(String caseId) {
        DemoCaseEntity demoCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new CaseNotFoundException(caseId));

        LicenseContractEntity contract = contractRepository.findByCaseId(caseId)
                .orElseThrow(() -> new IllegalStateException("Case is missing its fictional contract metadata"));

        List<EvidenceSummary> evidence = evidenceRepository.findAllByCaseIdOrderByIdAsc(caseId).stream()
                .map(item -> new EvidenceSummary(
                        item.getId(), item.getType(), item.getLabel(), item.getAdReceivedOn(), item.isOutOfContract()))
                .toList();

        ContractSummary contractSummary = new ContractSummary(
                contract.getId(), contract.getVersion(), contract.getTitle(), contract.getSummary(),
                contract.getContractValidThrough());

        return new CaseDetailResponse(
                demoCase.getId(), demoCase.getTitle(), demoCase.isFictional(), contractSummary, evidence);
    }

    private CaseSummaryResponse toSummary(DemoCaseEntity demoCase) {
        return new CaseSummaryResponse(
                demoCase.getId(), demoCase.getTitle(), demoCase.getScenarioType(),
                demoCase.getStatus(), demoCase.isFictional());
    }
}
