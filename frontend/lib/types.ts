export type ScenarioType = "POSSIBLE_MISMATCH" | "PERMITTED" | "AMBIGUOUS";
export type DecisionType = "APPROVE" | "REJECT";

export interface CaseSummary {
  caseId: string;
  title: string;
  scenarioType: ScenarioType;
  status: string;
  fictional: boolean;
}

export interface CaseDetail {
  caseId: string;
  title: string;
  fictional: boolean;
  contract: {
    contractId: string;
    version: number;
    title: string;
    summary: string;
    validThrough: string | null;
  };
  evidence: Array<{
    evidenceId: string;
    type: string;
    label: string;
    adReceivedOn: string | null;
    outOfContract: boolean;
  }>;
}

export interface FeeLineItem {
  evidenceId: string;
  contractValidThrough: string | null;
  adReceivedOn: string | null;
  billableDays: number;
  amount: string | null;
  actionable: boolean;
}

export interface AnalysisResult {
  analysisId: string;
  caseId: string;
  version: number;
  outcome: ScenarioType;
  summary: string;
  clauseReferences: string[];
  evidenceReferences: string[];
  uncertainties: string[];
  feeQuote: null | {
    feeQuoteId: string;
    amount: string | null;
    currency: string;
    dailyRate: string;
    billableDays: number;
    ruleReference: string;
    actionable: boolean;
    items: FeeLineItem[];
  };
  source: string;
  modelName: string | null;
  createdAt: string;
}

export interface DecisionResult {
  decisionId: string;
  caseId: string;
  decision: DecisionType;
  analysisId: string;
  feeQuoteId: string | null;
  decidedAt: string;
}

export interface InvoiceResult {
  invoiceRecordId: string;
  caseId: string;
  provider: "MOCK" | "PAYPAL";
  providerInvoiceId: string | null;
  status: string;
  amount: string | null;
  currency: string;
  approvalDecisionId: string;
  invoiceUrl: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AuditEntry {
  eventId: string;
  eventType: string;
  recordId: string | null;
  details: Record<string, unknown>;
  occurredAt: string;
}
