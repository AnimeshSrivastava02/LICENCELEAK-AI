"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";
import { canApprove, canCreateInvoice, formatMoney } from "@/lib/presentation";
import type { AnalysisResult, AuditEntry, CaseDetail, CaseSummary, DecisionResult, InvoiceResult } from "@/lib/types";

function shortDate(value: string | null | undefined) {
  if (!value) return "Not recorded";
  return new Intl.DateTimeFormat("en-US", { month: "short", day: "numeric", year: "numeric", timeZone: "UTC" })
    .format(new Date(`${value.slice(0, 10)}T00:00:00Z`));
}

function outcomeLabel(value: string) {
  return value.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function outcomeTone(value?: string) {
  if (value === "POSSIBLE_MISMATCH") return "tone-amber";
  if (value === "PERMITTED") return "tone-green";
  return "tone-violet";
}

function initials(title: string) {
  return caseTitle(title).split(" ").slice(0, 2).map((word) => word[0]).join("").toUpperCase();
}

function caseTitle(title: string) {
  return title.replace(/^Fictional campaign:\s*/i, "");
}

function displayCaseId(caseId: string) {
  return caseId.replace(/^case-demo-/, "Case ");
}

function displayEvidenceId(evidenceId: string) {
  return evidenceId.replace(/^evidence-demo-/, "Record ");
}

export default function Home() {
  const [cases, setCases] = useState<CaseSummary[]>([]);
  const [selectedId, setSelectedId] = useState("");
  const [detail, setDetail] = useState<CaseDetail | null>(null);
  const [analysis, setAnalysis] = useState<AnalysisResult | null>(null);
  const [decision, setDecision] = useState<DecisionResult | null>(null);
  const [invoice, setInvoice] = useState<InvoiceResult | null>(null);
  const [audit, setAudit] = useState<AuditEntry[]>([]);
  const [busy, setBusy] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [reloadVersion, setReloadVersion] = useState(0);

  useEffect(() => {
    api.cases()
      .then(({ items }) => {
        setCases(items);
        setSelectedId(items[0]?.caseId ?? "");
        if (items.length === 0) setLoading(false);
      })
      .catch((cause: unknown) => {
        setError(cause instanceof Error ? cause.message : "Could not load cases.");
        setLoading(false);
      });
  }, []);

  const loadRelated = useCallback(async (caseId: string, isCurrent: () => boolean = () => true) => {
    const [analysisResult, decisionResult, invoiceResult, auditResult] = await Promise.allSettled([
      api.latestAnalysis(caseId), api.latestDecision(caseId), api.invoice(caseId), api.audit(caseId),
    ]);
    if (!isCurrent()) return;
    setAnalysis(analysisResult.status === "fulfilled" ? analysisResult.value : null);
    setDecision(decisionResult.status === "fulfilled" ? decisionResult.value : null);
    setInvoice(invoiceResult.status === "fulfilled" ? invoiceResult.value : null);
    setAudit(auditResult.status === "fulfilled" ? auditResult.value.items : []);
  }, []);

  useEffect(() => {
    if (!selectedId) return;
    let current = true;
    setLoading(true);
    api.caseDetail(selectedId)
      .then(async (caseDetail) => {
        if (!current) return;
        setDetail(caseDetail);
        await loadRelated(selectedId, () => current);
      })
      .catch((cause: unknown) => {
        if (current) setError(cause instanceof Error ? cause.message : "Could not load the case.");
      })
      .finally(() => { if (current) setLoading(false); });
    return () => { current = false; };
  }, [selectedId, reloadVersion, loadRelated]);

  const selectCase = (caseId: string) => {
    setError("");
    if (caseId === selectedId) {
      setReloadVersion((version) => version + 1);
      return;
    }
    setLoading(true);
    setDetail(null);
    setAnalysis(null);
    setDecision(null);
    setInvoice(null);
    setAudit([]);
    setSelectedId(caseId);
  };

  const currentDecision = decision?.analysisId === analysis?.analysisId ? decision : null;
  const approvalAllowed = canApprove(analysis);
  const invoiceAllowed = canCreateInvoice(analysis, currentDecision) && invoice === null;

  async function runAnalysis() {
    if (!selectedId) return;
    setBusy("analysis"); setError("");
    try {
      const result = await api.runAnalysis(selectedId);
      setAnalysis(result); setDecision(null); setInvoice(null);
      await loadRelated(selectedId);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Analysis could not be completed.");
    } finally { setBusy(""); }
  }

  async function submitDecision(value: "APPROVE" | "REJECT") {
    if (!selectedId || !analysis) return;
    setBusy(value.toLowerCase()); setError("");
    try {
      const result = await api.decide(selectedId, value, analysis.analysisId,
        value === "APPROVE" ? analysis.feeQuote?.feeQuoteId : undefined);
      setDecision(result); setInvoice(null);
      await loadRelated(selectedId);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Your decision could not be saved.");
    } finally { setBusy(""); }
  }

  async function createInvoice() {
    if (!selectedId) return;
    setBusy("invoice"); setError("");
    try {
      setInvoice(await api.createInvoice(selectedId));
      await loadRelated(selectedId);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Invoice could not be created.");
      await loadRelated(selectedId);
    } finally { setBusy(""); }
  }

  async function refreshInvoiceStatus() {
    if (!selectedId || !invoice) return;
    setBusy("invoice-status"); setError("");
    try {
      setInvoice(await api.invoice(selectedId));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Invoice status could not be refreshed.");
    } finally { setBusy(""); }
  }

  async function refreshAudit() {
    if (!selectedId) return;
    try { setAudit((await api.audit(selectedId)).items); } catch { /* audit is optional when loading a case */ }
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#home" aria-label="LicenseLeak AI home">
          <span className="brand-mark"><span /></span>
          <span className="brand-name">license<span>leak</span><small>CREATOR DESK</small></span>
        </a>
        <div className="workspace-label">WORKSPACE</div>
        <nav className="side-nav" aria-label="Main navigation">
          <a className="nav-link active" href="#cases"><span className="nav-icon">▦</span> Cases <span className="nav-count">{cases.length}</span></a>
          <a className="nav-link" href="#activity"><span className="nav-icon">◷</span> Activity</a>
        </nav>
        <div className="side-spacer" />
        <div className="sidebar-note"><span className="online-dot" /> Creator workspace <small>Records</small></div>
        <div className="profile"><div className="avatar">CO</div><div><strong>Workspace owner</strong><small>Creator account</small></div><span className="profile-menu">···</span></div>
      </aside>

      <section className="main-area" id="home">
        <header className="topbar">
          <div className="crumbs"><span>Workspace</span><b>/</b><strong>Cases</strong></div>
          <div className="topbar-right"><span className="sandbox-pill"><i /> SANDBOX</span><button className="icon-button" aria-label="Notifications">♧<span className="notification-dot" /></button><div className="avatar avatar-small">CO</div></div>
        </header>

        <div className="content-wrap" id="cases">
          <div className="page-heading">
            <div><div className="eyebrow">CREATOR LICENSING REVIEW</div><h1>Campaign rights review <span>✳</span></h1><p>Review license terms, usage evidence, and payment status in one place.</p></div>
            <button className="button button-primary" onClick={runAnalysis} disabled={!detail || Boolean(busy)}><span>✦</span> {busy === "analysis" ? "Reviewing…" : "Run analysis"}</button>
          </div>

          <div className="data-notice"><span className="banner-icon">i</span><div className="notice-copy"><strong>Records</strong><span>Current case materials are illustrative. Verify original agreements and evidence before relying on a review.</span></div></div>

          {error && <div className="alert" role="alert"><span>!</span>{error}<button onClick={() => setError("")} aria-label="Dismiss error">×</button></div>}

          <div className="dashboard-grid">
            <section className="case-column" aria-labelledby="case-list-title">
              <div className="section-heading"><div><h2 id="case-list-title">Your cases <span className="soft-count">{cases.length}</span></h2><p>Choose a campaign to review</p></div><button className="small-icon-button" aria-label="More case options">···</button></div>
              <div className="case-list">
                {loading && cases.length === 0 ? <div className="loading-card">Loading your cases…</div> : null}
                {cases.map((item, index) => (
                  <button key={item.caseId} onClick={() => selectCase(item.caseId)} className={`case-card ${selectedId === item.caseId ? "selected" : ""}`} aria-pressed={selectedId === item.caseId}>
                    <span className={`case-art art-${index + 1}`}><span>{initials(item.title)}</span><i>✳</i></span>
                    <span className="case-copy"><strong>{caseTitle(item.title)}</strong><span>{displayCaseId(item.caseId)} · Campaign review</span><span className={`scenario-label ${outcomeTone(item.scenarioType)}`}><i />{outcomeLabel(item.scenarioType)}</span></span>
                    <span className="case-arrow">›</span>
                  </button>
                ))}
              </div>
              <div className="sidebar-tip"><span>✦</span><div><strong>Keep your work clear</strong><p>Every result links back to the contract terms and evidence considered.</p></div></div>
            </section>

            <section className="review-column" aria-label="Selected case review">
              {!detail && !loading ? <div className="empty-state"><span>▧</span><h2>Select a case to begin</h2><p>Choose a campaign record on the left.</p></div> : null}
              {detail && <>
                <div className="selected-heading"><div><div className="eyebrow">SELECTED CAMPAIGN</div><h2>{caseTitle(detail.title)}</h2><div className="case-meta"><span className="case-chip">CASE FILE</span><span>{displayCaseId(detail.caseId)}</span><span>·</span><span>{analysis ? `Analysis v${analysis.version}` : "Not analyzed yet"}</span></div></div><button className="small-icon-button" aria-label="More case actions">···</button></div>

                <div className="review-card contract-card">
                  <div className="card-head"><div className="card-title-icon contract-icon">⌑</div><div><span className="card-kicker">LICENSE AGREEMENT</span><h3>{detail.contract.title}</h3></div><span className="version-chip">v{detail.contract.version}</span></div>
                  <p className="contract-text" id="contract-terms">{detail.contract.summary}</p>
                  <div className="contract-foot"><span><i className="mini-icon">▤</i> Contract period ends <strong>{shortDate(detail.contract.validThrough)}</strong></span><a href="#contract-terms">Review terms <b>↗</b></a></div>
                </div>

                <div className="review-card evidence-card" id="evidence">
                  <div className="card-head"><div className="card-title-icon evidence-icon">▣</div><div><span className="card-kicker">USAGE EVIDENCE</span><h3>Campaign materials <span className="soft-count">{detail.evidence.length}</span></h3></div><span className="evidence-status"><i /> {detail.evidence.some((item) => item.outOfContract) ? "Needs review" : "In period"}</span></div>
                  {detail.evidence.map((item) => <div className="evidence-row" key={item.evidenceId}><div className="evidence-thumb">POST</div><div className="evidence-info"><strong>{item.label}</strong><span>{item.type.replaceAll("_", " ")} · Observed {shortDate(item.adReceivedOn)} · {displayEvidenceId(item.evidenceId)}</span></div><span className={`evidence-mark ${item.outOfContract ? "mark-amber" : "mark-green"}`}>{item.outOfContract ? "Review" : "Within term"}</span></div>)}
                  <div className="evidence-note"><span>i</span> Verify the original source and capture date before making a licensing decision.</div>
                </div>

                <div className="review-card analysis-card">
                  <div className="card-head"><div className="card-title-icon analysis-icon">✦</div><div><span className="card-kicker">ASSISTED REVIEW</span><h3>Analysis {analysis ? <span className="version-chip">v{analysis.version}</span> : null}</h3></div><span className="source-chip">{analysis?.source === "GEMINI" ? "GEMINI" : analysis ? "ASSISTED REVIEW" : "READY"}</span></div>
                  {!analysis ? <div className="analysis-empty"><div className="sparkle">✳</div><div><strong>Ready to review this case</strong><p>Generate a structured review with references and uncertainty notes.</p></div><button className="text-button" onClick={runAnalysis} disabled={Boolean(busy)}>{busy === "analysis" ? "Working…" : "Start review →"}</button></div> : <>
                    <div className="analysis-result"><div className={`outcome-badge ${outcomeTone(analysis.outcome)}`}><i />{outcomeLabel(analysis.outcome)}</div><p>{analysis.summary}</p></div>
                    <div className="reference-grid"><div><span className="reference-label">CLAUSES</span><div className="reference-pills">{analysis.clauseReferences.length ? analysis.clauseReferences.map((ref) => <span key={ref}>{ref}</span>) : <em>None cited</em>}</div></div><div><span className="reference-label">EVIDENCE</span><div className="reference-pills">{analysis.evidenceReferences.length ? analysis.evidenceReferences.map((ref) => <span key={ref}>{ref}</span>) : <em>None cited</em>}</div></div></div>
                    {analysis.uncertainties.length > 0 && <div className="uncertainty-box"><span>!</span><div><strong>Needs human review</strong><ul>{analysis.uncertainties.map((note) => <li key={note}>{note}</li>)}</ul></div></div>}
                    <div className="fee-panel"><div className="fee-title"><div><span className="card-kicker">POTENTIAL FEE</span><strong>{analysis.feeQuote?.amount ? formatMoney(analysis.feeQuote.amount, analysis.feeQuote.currency) : "No actionable quote"}</strong></div><span className={`quote-state ${analysis.feeQuote?.actionable ? "quote-ready" : "quote-review"}`}>{analysis.feeQuote?.actionable ? "READY TO REVIEW" : "NOT ACTIONABLE"}</span></div>
                      {analysis.feeQuote && <><div className="fee-rule">${analysis.feeQuote.dailyRate} × {analysis.feeQuote.billableDays} billable day(s) per ad</div><div className="fee-items">{analysis.feeQuote.items.map((line) => <div key={line.evidenceId}><span>{line.evidenceId}: {shortDate(line.contractValidThrough)} → {shortDate(line.adReceivedOn)}</span><strong>{line.amount ? formatMoney(line.amount, analysis.feeQuote!.currency) : "Needs review"}</strong></div>)}</div></>}
                    </div>
                    <div className="decision-area"><div><strong>Creator decision</strong><span>{currentDecision ? `${currentDecision.decision === "APPROVE" ? "Approved" : "Rejected"} · ${shortDate(currentDecision.decidedAt)}` : "Review the result before deciding"}</span></div><div className="decision-actions"><button className="button button-quiet" onClick={() => submitDecision("REJECT")} disabled={Boolean(busy)}>{busy === "reject" ? "Saving…" : "Reject"}</button><button className="button button-approve" onClick={() => submitDecision("APPROVE")} disabled={!approvalAllowed || Boolean(busy)}>{busy === "approve" ? "Saving…" : "Approve review"}</button></div></div>
                  </>}
                </div>

                <div className="review-card invoice-card"><div className="invoice-top"><div className="card-title-icon paypal-icon">P</div><div><span className="card-kicker">PAYMENT REQUEST</span><h3>PayPal invoice</h3><p>{invoice ? `${invoice.provider} · ${invoice.status}` : "A current approval and actionable quote are required."}</p></div></div>
                  {invoice ? <div className="invoice-result"><div><span>Invoice status</span><strong aria-live="polite" className={`invoice-status status-${invoice.status.toLowerCase()}`}>{invoice.status}</strong></div><div><span>Amount</span><strong>{formatMoney(invoice.amount, invoice.currency)}</strong></div><div><span>Provider ID</span><strong className="provider-id">{invoice.providerInvoiceId ?? "Pending confirmation"}</strong></div>{invoice.provider === "PAYPAL" && <div className="invoice-actions"><button className="button button-refresh" onClick={refreshInvoiceStatus} disabled={Boolean(busy)} aria-label="Refresh invoice status">{busy === "invoice-status" ? "Refreshing status…" : "Refresh status"}<span aria-hidden="true">↻</span></button></div>}{invoice.provider === "MOCK" && <p className="mock-notice">Local test record. No PayPal invoice was issued.</p>}{invoice.invoiceUrl && <a href={invoice.invoiceUrl} target="_blank" rel="noreferrer" className="text-button">Open invoice ↗</a>}{invoice.provider === "PAYPAL" && invoice.providerInvoiceId && ["UNKNOWN", "FAILED"].includes(invoice.status) && <div className="invoice-actions"><button className="button button-paypal" onClick={createInvoice} disabled={Boolean(busy)}>{busy === "invoice" ? "Checking PayPal draft…" : "Check draft and retry sending"}<span>→</span></button></div>}</div> : <button className="button button-paypal" onClick={createInvoice} disabled={!invoiceAllowed || Boolean(busy)}><span className="paypal-monogram">P</span>{busy === "invoice" ? "Creating invoice…" : "Create PayPal invoice"}<span>→</span></button>}
                  <div className="invoice-foot"><span>🔒</span> Invoices are only created after backend approval checks.</div>
                </div>

                <div className="review-card history-card" id="activity"><div className="card-head"><div className="card-title-icon history-icon">◷</div><div><span className="card-kicker">CASE HISTORY</span><h3>Recent activity</h3></div><button className="text-button" onClick={refreshAudit}>Refresh</button></div>
                  {audit.length === 0 ? <p className="history-empty">No recorded actions yet. Analysis, decisions, and invoice events will appear here.</p> : <ol className="timeline">{audit.slice(0, 6).map((entry) => <li key={entry.eventId}><span className="timeline-dot" /><div><strong>{entry.eventType.replaceAll("_", " ").toLowerCase()}</strong><span>{entry.recordId ?? entry.eventId} · {new Intl.DateTimeFormat("en-US", { dateStyle: "medium", timeStyle: "short" }).format(new Date(entry.occurredAt))}</span></div></li>)}</ol>}
                </div>
              </>}
            </section>
          </div>

          <footer className="page-footer"><span>LicenseLeak AI</span><span>Creator review workspace · Evidence-linked decisions</span><a href="#home">Privacy & safety</a></footer>
        </div>
      </section>
    </main>
  );
}
