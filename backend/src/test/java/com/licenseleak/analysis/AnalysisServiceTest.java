package com.licenseleak.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.licenseleak.analysis.application.AnalysisDraft;
import com.licenseleak.analysis.application.AnalysisInput;
import com.licenseleak.analysis.application.AnalysisProvider;
import com.licenseleak.analysis.application.AnalysisService;
import com.licenseleak.analysis.domain.AnalysisOutcome;
import com.licenseleak.analysis.domain.AnalysisRunEntity;
import com.licenseleak.analysis.infrastructure.AnalysisRunRepository;
import com.licenseleak.audit.application.AuditService;
import com.licenseleak.cases.domain.CaseScenarioType;
import com.licenseleak.cases.domain.DemoCaseEntity;
import com.licenseleak.cases.domain.LicenseContractEntity;
import com.licenseleak.cases.domain.UsageEvidenceEntity;
import com.licenseleak.cases.infrastructure.DemoCaseRepository;
import com.licenseleak.cases.infrastructure.LicenseContractRepository;
import com.licenseleak.cases.infrastructure.UsageEvidenceRepository;
import com.licenseleak.fees.application.DailyFeeCalculator;
import com.licenseleak.fees.domain.FeeQuoteEntity;
import com.licenseleak.fees.infrastructure.FeeQuoteRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class AnalysisServiceTest {

    @Test
    void savesPerAdQuoteUsingValidatedSourceDates() {
        var provider = mock(AnalysisProvider.class);
        var analysisRepository = mock(AnalysisRunRepository.class);
        var caseRepository = mock(DemoCaseRepository.class);
        var contractRepository = mock(LicenseContractRepository.class);
        var evidenceRepository = mock(UsageEvidenceRepository.class);
        var quoteRepository = mock(FeeQuoteRepository.class);
        var auditService = mock(AuditService.class);
        var transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        var demoCase = mock(DemoCaseEntity.class);
        when(demoCase.getTitle()).thenReturn("Fictional campaign");
        when(demoCase.getScenarioType()).thenReturn(CaseScenarioType.POSSIBLE_MISMATCH);
        when(caseRepository.findById("case-demo-001")).thenReturn(Optional.of(demoCase));

        var contract = mock(LicenseContractEntity.class);
        when(contract.getVersion()).thenReturn(1);
        when(contract.getTitle()).thenReturn("Fictional licence");
        when(contract.getSummary()).thenReturn("clause-4: campaign ends on 2026-06-30");
        when(contract.getContractValidThrough()).thenReturn(LocalDate.parse("2026-06-30"));
        when(contractRepository.findByCaseId("case-demo-001")).thenReturn(Optional.of(contract));

        var evidence = mock(UsageEvidenceEntity.class);
        when(evidence.getId()).thenReturn("evidence-demo-001");
        when(evidence.getType()).thenReturn("CAMPAIGN_POST");
        when(evidence.getLabel()).thenReturn("Fictional ad");
        when(evidence.getAdReceivedOn()).thenReturn(LocalDate.parse("2026-07-03"));
        when(evidence.isOutOfContract()).thenReturn(true);
        when(evidenceRepository.findAllByCaseIdOrderByIdAsc("case-demo-001")).thenReturn(List.of(evidence));

        when(provider.analyze(any(AnalysisInput.class))).thenReturn(new AnalysisDraft(
                AnalysisOutcome.POSSIBLE_MISMATCH, "Possible mismatch based on fictional dates.",
                List.of("clause-4"), List.of("evidence-demo-001"), List.of(), "MOCK", "test"));
        when(analysisRepository.countByCaseId("case-demo-001")).thenReturn(0);
        when(analysisRepository.save(any(AnalysisRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(quoteRepository.save(any(FeeQuoteEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new AnalysisService(provider, analysisRepository, caseRepository, contractRepository,
                evidenceRepository, quoteRepository, new DailyFeeCalculator(new BigDecimal("10.00"), "USD"), auditService,
                transactionManager);
        var response = service.analyze("case-demo-001");

        assertEquals("POSSIBLE_MISMATCH", response.outcome());
        assertNotNull(response.feeQuote());
        assertEquals(3, response.feeQuote().billableDays());
        assertEquals("30.00", response.feeQuote().amount());
        assertEquals("evidence-demo-001", response.feeQuote().items().getFirst().evidenceId());
    }

    @Test
    void turnsUnknownModelEvidenceReferenceIntoAmbiguousHumanReview() {
        var provider = mock(AnalysisProvider.class);
        var analysisRepository = mock(AnalysisRunRepository.class);
        var caseRepository = mock(DemoCaseRepository.class);
        var contractRepository = mock(LicenseContractRepository.class);
        var evidenceRepository = mock(UsageEvidenceRepository.class);
        var quoteRepository = mock(FeeQuoteRepository.class);
        var auditService = mock(AuditService.class);
        var transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        var demoCase = mock(DemoCaseEntity.class);
        when(demoCase.getTitle()).thenReturn("Fictional campaign");
        when(demoCase.getScenarioType()).thenReturn(CaseScenarioType.POSSIBLE_MISMATCH);
        when(caseRepository.findById("case-demo-001")).thenReturn(Optional.of(demoCase));
        var contract = mock(LicenseContractEntity.class);
        when(contract.getVersion()).thenReturn(1);
        when(contract.getTitle()).thenReturn("Fictional licence");
        when(contract.getSummary()).thenReturn("clause-4: campaign dates");
        when(contract.getContractValidThrough()).thenReturn(LocalDate.parse("2026-06-30"));
        when(contractRepository.findByCaseId("case-demo-001")).thenReturn(Optional.of(contract));
        var evidence = mock(UsageEvidenceEntity.class);
        when(evidence.getId()).thenReturn("evidence-demo-001");
        when(evidence.getType()).thenReturn("CAMPAIGN_POST");
        when(evidence.getLabel()).thenReturn("Sample");
        when(evidence.getAdReceivedOn()).thenReturn(LocalDate.parse("2026-07-03"));
        when(evidence.isOutOfContract()).thenReturn(true);
        when(evidenceRepository.findAllByCaseIdOrderByIdAsc("case-demo-001")).thenReturn(List.of(evidence));
        when(provider.analyze(any(AnalysisInput.class))).thenReturn(new AnalysisDraft(
                AnalysisOutcome.POSSIBLE_MISMATCH, "Possible mismatch.", List.of("clause-4"),
                List.of("evidence-from-another-case"), List.of(), "GEMINI", "test"));
        when(analysisRepository.countByCaseId("case-demo-001")).thenReturn(0);
        when(analysisRepository.save(any(AnalysisRunEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new AnalysisService(provider, analysisRepository, caseRepository, contractRepository,
                evidenceRepository, quoteRepository, new DailyFeeCalculator(new BigDecimal("10.00"), "USD"), auditService,
                transactionManager);
        var response = service.analyze("case-demo-001");

        assertEquals("AMBIGUOUS", response.outcome());
        assertNull(response.feeQuote());
        assertEquals("VALIDATION", response.source());
    }
}
