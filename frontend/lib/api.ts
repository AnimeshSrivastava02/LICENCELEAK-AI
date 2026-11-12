import type {
  AnalysisResult,
  AuditEntry,
  CaseDetail,
  CaseSummary,
  DecisionResult,
  DecisionType,
  InvoiceResult,
} from "./types";

const API_BASE = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080/api/v1").replace(/\/$/, "");

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE}${path}`, {
    ...init,
    headers: { "Content-Type": "application/json", ...init?.headers },
    cache: "no-store",
  });
  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = (await response.json()) as { message?: string };
      message = body.message ?? message;
    } catch {
      // Keep the safe status-based message when the server body is not JSON.
    }
    throw new Error(message);
  }
  return response.json() as Promise<T>;
}

export const api = {
  cases: () => request<{ items: CaseSummary[] }>("/cases"),
  caseDetail: (caseId: string) => request<CaseDetail>(`/cases/${encodeURIComponent(caseId)}`),
  latestAnalysis: (caseId: string) => request<AnalysisResult>(`/cases/${encodeURIComponent(caseId)}/analysis`),
  runAnalysis: (caseId: string) => request<AnalysisResult>(`/cases/${encodeURIComponent(caseId)}/analysis`, { method: "POST" }),
  latestDecision: (caseId: string) => request<DecisionResult>(`/cases/${encodeURIComponent(caseId)}/decision`),
  decide: (caseId: string, decision: DecisionType, analysisId: string, feeQuoteId?: string) =>
    request<DecisionResult>(`/cases/${encodeURIComponent(caseId)}/decision`, {
      method: "POST",
      body: JSON.stringify({ decision, analysisId, ...(feeQuoteId ? { feeQuoteId } : {}) }),
    }),
  invoice: (caseId: string) => request<InvoiceResult>(`/cases/${encodeURIComponent(caseId)}/invoice`),
  createInvoice: (caseId: string) => request<InvoiceResult>(`/cases/${encodeURIComponent(caseId)}/invoice`, { method: "POST" }),
  audit: (caseId: string) => request<{ items: AuditEntry[] }>(`/cases/${encodeURIComponent(caseId)}/audit`),
};
