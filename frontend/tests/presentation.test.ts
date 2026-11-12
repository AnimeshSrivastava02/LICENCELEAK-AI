import { describe, expect, it } from "vitest";
import { canApprove, canCreateInvoice, formatMoney } from "../lib/presentation";
import type { AnalysisResult, DecisionResult } from "../lib/types";

function sampleAnalysis(overrides: Partial<AnalysisResult> = {}): AnalysisResult {
  return {
    analysisId: "analysis-1",
    caseId: "case-1",
    version: 1,
    outcome: "POSSIBLE_MISMATCH",
    summary: "Fictional sample review.",
    clauseReferences: ["clause-4"],
    evidenceReferences: ["evidence-1"],
    uncertainties: [],
    feeQuote: {
      feeQuoteId: "quote-1",
      amount: "30.00",
      currency: "USD",
      dailyRate: "10.00",
      billableDays: 3,
      ruleReference: "DAILY_RATE_PER_BILLABLE_DAY",
      actionable: true,
      items: [],
    },
    source: "MOCK",
    modelName: "fictional-scenario-mock",
    createdAt: "2026-11-11T00:00:00Z",
    ...overrides,
  };
}

const approved: DecisionResult = {
  decisionId: "decision-1",
  caseId: "case-1",
  decision: "APPROVE",
  analysisId: "analysis-1",
  feeQuoteId: "quote-1",
  decidedAt: "2026-11-11T00:00:00Z",
};

describe("review action guards", () => {
  it("allows approval for a mismatch only when its quote is actionable", () => {
    expect(canApprove(sampleAnalysis())).toBe(true);
    expect(canApprove(sampleAnalysis({ feeQuote: { ...sampleAnalysis().feeQuote!, actionable: false } }))).toBe(false);
  });

  it("requires approval for the exact analysis and quote before invoicing", () => {
    expect(canCreateInvoice(sampleAnalysis(), approved)).toBe(true);
    expect(canCreateInvoice(sampleAnalysis({ analysisId: "analysis-2" }), approved)).toBe(false);
    expect(canCreateInvoice(sampleAnalysis(), { ...approved, decision: "REJECT" })).toBe(false);
    expect(canCreateInvoice(sampleAnalysis(), { ...approved, feeQuoteId: "quote-old" })).toBe(false);
  });

  it("formats the displayed fee in the quote currency", () => {
    expect(formatMoney("30.00", "USD")).toContain("30.00");
    expect(formatMoney(null)).toBe("Needs review");
  });
});
