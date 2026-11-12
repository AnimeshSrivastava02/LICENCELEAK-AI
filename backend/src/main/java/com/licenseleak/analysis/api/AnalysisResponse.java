package com.licenseleak.analysis.api;

import com.licenseleak.analysis.application.AnalysisDraft;
import com.licenseleak.analysis.domain.AnalysisRunEntity;
import com.licenseleak.fees.domain.FeeQuoteEntity;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AnalysisResponse(
        String analysisId,
        String caseId,
        int version,
        String outcome,
        String summary,
        List<String> clauseReferences,
        List<String> evidenceReferences,
        List<String> uncertainties,
        FeeQuoteResponse feeQuote,
        String source,
        String modelName,
        OffsetDateTime createdAt) {

    public static AnalysisResponse from(AnalysisRunEntity analysis, FeeQuoteEntity quote) {
        return new AnalysisResponse(
                analysis.getId(), analysis.getCaseId(), analysis.getVersion(), analysis.getOutcome().name(),
                analysis.getSummary(), analysis.getClauseReferences(), analysis.getEvidenceReferences(),
                analysis.getUncertainties(), quote == null ? null : FeeQuoteResponse.from(quote),
                analysis.getSource(), analysis.getModelName(), analysis.getCreatedAt());
    }

    public record FeeQuoteResponse(
            String feeQuoteId,
            String amount,
            String currency,
            String dailyRate,
            int billableDays,
            String ruleReference,
            boolean actionable,
            List<FeeLineItem> items) {
        static FeeQuoteResponse from(FeeQuoteEntity quote) {
            return new FeeQuoteResponse(quote.getId(),
                    quote.getAmount() == null ? null : quote.getAmount().toPlainString(),
                    quote.getCurrency(), quote.getDailyRate().toPlainString(), quote.getTotalBillableDays(),
                    quote.getRuleReference(), quote.isActionable(), quote.getItems().stream()
                    .map(FeeLineItem::from).toList());
        }
    }

    public record FeeLineItem(
            String evidenceId,
            LocalDate contractValidThrough,
            LocalDate adReceivedOn,
            int billableDays,
            String amount,
            boolean actionable) {
        static FeeLineItem from(com.licenseleak.fees.domain.FeeQuoteItemEntity item) {
            return new FeeLineItem(item.getEvidenceId(), item.getContractValidThrough(), item.getAdReceivedOn(),
                    item.getBillableDays(), item.getAmount() == null ? null : item.getAmount().toPlainString(),
                    item.isActionable());
        }
    }
}
