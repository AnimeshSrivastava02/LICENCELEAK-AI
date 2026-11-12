import type { AnalysisResult, DecisionResult } from "./types";

export function canApprove(analysis: AnalysisResult | null): boolean {
  if (!analysis || analysis.outcome === "AMBIGUOUS") return false;
  if (analysis.outcome === "PERMITTED") return true;
  return analysis.feeQuote?.actionable === true;
}

export function canCreateInvoice(
  analysis: AnalysisResult | null,
  decision: DecisionResult | null,
): boolean {
  const quote = analysis?.feeQuote;
  if (!analysis || !quote || !decision) return false;
  return quote.actionable && decision.decision === "APPROVE" &&
    decision.analysisId === analysis.analysisId && decision.feeQuoteId === quote.feeQuoteId;
}

export function formatMoney(amount: string | null | undefined, currency = "USD"): string {
  if (amount == null) return "Needs review";
  return new Intl.NumberFormat("en-US", { style: "currency", currency }).format(Number(amount));
}
