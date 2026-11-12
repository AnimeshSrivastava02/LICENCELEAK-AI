package com.licenseleak.approvals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.licenseleak.analysis.domain.AnalysisOutcome;
import com.licenseleak.analysis.domain.AnalysisRunEntity;
import com.licenseleak.analysis.infrastructure.AnalysisRunRepository;
import com.licenseleak.approvals.api.DecisionRequest;
import com.licenseleak.approvals.application.DecisionService;
import com.licenseleak.approvals.domain.CreatorDecisionEntity;
import com.licenseleak.approvals.domain.DecisionType;
import com.licenseleak.approvals.infrastructure.CreatorDecisionRepository;
import com.licenseleak.audit.application.AuditService;
import com.licenseleak.cases.infrastructure.DemoCaseRepository;
import com.licenseleak.common.errors.ConflictException;
import com.licenseleak.fees.domain.FeeQuoteEntity;
import com.licenseleak.fees.infrastructure.FeeQuoteRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DecisionServiceTest {

    @Test
    void rejectsDecisionForStaleAnalysis() {
        var cases = mock(DemoCaseRepository.class);
        var analyses = mock(AnalysisRunRepository.class);
        var quotes = mock(FeeQuoteRepository.class);
        var decisions = mock(CreatorDecisionRepository.class);
        var audit = mock(AuditService.class);
        when(cases.existsById("case-demo-001")).thenReturn(true);
        when(analyses.findFirstByCaseIdOrderByVersionDesc("case-demo-001"))
                .thenReturn(Optional.of(analysis("analysis-current")));

        var service = new DecisionService(cases, analyses, quotes, decisions, audit);
        var error = assertThrows(ConflictException.class, () -> service.decide("case-demo-001",
                new DecisionRequest(DecisionType.APPROVE, "analysis-old", "quote-id")));

        assertEquals("STALE_ANALYSIS", error.getCode());
        verify(decisions, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void recordsDecisionAgainstLatestAnalysisAndQuote() {
        var cases = mock(DemoCaseRepository.class);
        var analyses = mock(AnalysisRunRepository.class);
        var quotes = mock(FeeQuoteRepository.class);
        var decisions = mock(CreatorDecisionRepository.class);
        var audit = mock(AuditService.class);
        when(cases.existsById("case-demo-001")).thenReturn(true);
        when(analyses.findFirstByCaseIdOrderByVersionDesc("case-demo-001"))
                .thenReturn(Optional.of(analysis("analysis-current")));
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var quote = new FeeQuoteEntity("quote-current", "analysis-current", new java.math.BigDecimal("30.00"),
                "USD", new java.math.BigDecimal("10.00"), 3, true, "DAILY_RATE_PER_BILLABLE_DAY", now, List.of());
        when(quotes.findByIdAndAnalysisId("quote-current", "analysis-current")).thenReturn(Optional.of(quote));
        when(decisions.save(any(CreatorDecisionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var service = new DecisionService(cases, analyses, quotes, decisions, audit);
        var response = service.decide("case-demo-001",
                new DecisionRequest(DecisionType.APPROVE, "analysis-current", "quote-current"));

        assertEquals("APPROVE", response.decision());
        assertEquals("analysis-current", response.analysisId());
        assertEquals("quote-current", response.feeQuoteId());
        verify(decisions).save(any(CreatorDecisionEntity.class));
    }

    private AnalysisRunEntity analysis(String id) {
        return new AnalysisRunEntity(id, "case-demo-001", 1, AnalysisOutcome.POSSIBLE_MISMATCH,
                "Fictional sample", "MOCK", "test", 1, OffsetDateTime.now(ZoneOffset.UTC),
                List.of("clause-4"), List.of("evidence-demo-001"), List.of());
    }
}
