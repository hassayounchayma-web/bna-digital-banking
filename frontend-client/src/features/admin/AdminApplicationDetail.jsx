import { useState } from "react";
import {
  ArrowLeft,
  Check,
  CircleHelp,
  ClipboardCheck,
  FileText,
  History,
  Scale,
  ShieldCheck,
  Sparkles,
} from "lucide-react";
import { applicationScenarioLabel, applicationStatusLabel } from "../applications/statusLabels";

const currency = new Intl.NumberFormat("fr-TN", {
  style: "currency",
  currency: "TND",
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});
const percent = new Intl.NumberFormat("fr-TN", { maximumFractionDigits: 3 });

const tabs = [
  { id: "financial", label: "Informations financières", icon: FileText },
  { id: "scoring", label: "Scoring Java", icon: ShieldCheck },
  { id: "analysis", label: "Analyse & comparaison", icon: Scale },
  { id: "decision", label: "Décision & historique", icon: History },
];

function eligibilityLabel(value) {
  return ({
    ELIGIBLE: "Éligible selon l’analyse",
    INELIGIBLE: "Non éligible selon l’analyse",
    INCONCLUSIVE: "Indéterminé",
  })[value] || "Indéterminé";
}

function riskLabel(value) {
  return ({
    LOW: "Faible",
    MODERATE: "Modéré",
    HIGH: "Élevé",
    UNKNOWN: "Indéterminé",
  })[value] || "Indéterminé";
}

function agreementLabel(value) {
  return value === true ? "Accord" : value === false ? "Divergence" : "Non comparable";
}

function Fact({ label, children }) {
  return <div className="admin-detail-fact"><dt>{label}</dt><dd>{children ?? "—"}</dd></div>;
}

function scenarioLabel(scenario) {
  return ({
    SANS_RACHAT: "Sans rachat",
    RACHAT_INTERNE: "Rachat interne",
    RACHAT_EXTERNE: "Rachat externe",
  })[scenario] || applicationScenarioLabel(scenario);
}

function SectionHeading({ eyebrow, title, icon: Icon }) {
  return <header className="admin-section-heading"><span className="admin-section-icon"><Icon size={18} /></span><div><p className="eyebrow">{eyebrow}</p><h2>{title}</h2></div></header>;
}

function FinancialTab({ application }) {
  const simulation = application.simulation;
  return (
    <div className="admin-tab-content">
      <section className="card admin-detail-section">
        <SectionHeading eyebrow="Demande et financement" title="Informations générales" icon={ClipboardCheck} />
        <dl className="admin-facts-grid">
          <Fact label="Produit demandé">{application.creditType || "Produit non renseigné"}</Fact>
          <Fact label="Scénario">{scenarioLabel(application.scenario)}</Fact>
          <Fact label="Montant demandé">{application.requestedAmount == null ? "Montant à préciser" : currency.format(application.requestedAmount)}</Fact>
          <Fact label="Durée">{application.durationMonths ? `${application.durationMonths} mois` : "—"}</Fact>
          <Fact label="Créée le">{new Date(application.createdAt).toLocaleDateString("fr-TN", { dateStyle: "medium" })}</Fact>
          <Fact label="Soumise le">{application.submittedAt ? new Date(application.submittedAt).toLocaleDateString("fr-TN", { dateStyle: "medium" }) : "Non soumise"}</Fact>
        </dl>
      </section>
      <section className="card admin-detail-section">
        <SectionHeading eyebrow="Capacité de remboursement" title="Revenus, charges et simulation" icon={Scale} />
        <dl className="admin-facts-grid">
          <Fact label="Salaire net moyen">{currency.format(application.averageMonthlySalary)}</Fact>
          <Fact label="Loyer retenu (50 %)">{currency.format(application.halfRentIncome)}</Fact>
          <Fact label="Retenues SED">{currency.format(application.sedDeductions)}</Fact>
          <Fact label="Retenues crédits BNA">{currency.format(application.bnaDeductions)}</Fact>
          <Fact label="Retenues BNA non rachetées">{currency.format(application.bnaNonBuyoutDeductions)}</Fact>
          <Fact label="Capital restant à racheter">{currency.format(application.remainingBuyoutCapital)}</Fact>
          <Fact label="Revenu retenu">{simulation?.netIncome == null ? "—" : currency.format(simulation.netIncome)}</Fact>
          <Fact label="Taux d’endettement">{simulation?.debtRatePercent == null ? "—" : `${percent.format(simulation.debtRatePercent)} %`}</Fact>
          <Fact label="Garde-fou">{simulation?.safeguardPercent == null ? "—" : `${percent.format(simulation.safeguardPercent)} %`}</Fact>
          <Fact label="Capacité mensuelle">{simulation?.monthlyCapacity == null ? "—" : currency.format(simulation.monthlyCapacity)}</Fact>
          <Fact label="Montant maximal calculé">{simulation?.maximumAmount == null ? "—" : currency.format(simulation.maximumAmount)}</Fact>
          <Fact label="Taux annuel">{simulation?.annualRatePercent == null ? "—" : `${percent.format(simulation.annualRatePercent)} %`}</Fact>
          <Fact label="Mensualité estimée">{simulation?.monthlyPayment == null ? "—" : currency.format(simulation.monthlyPayment)}</Fact>
        </dl>
        <p className="admin-simulation-status">Résultat de simulation : <strong>{simulation?.status ?? "Non disponible"}</strong>{simulation?.message ? ` — ${simulation.message}` : ""}</p>
      </section>
    </div>
  );
}

function ScoringTab({ scoring }) {
  if (!scoring) return <div className="card admin-empty-analysis"><CircleHelp size={22} /><p>Aucun scoring Java disponible pour cette demande.</p></div>;
  const score = Number(scoring.scoreOutOf100);
  const riskText = riskLabel(scoring.riskLevel);
  return (
    <div className="admin-tab-content">
      <section className="card admin-score-overview">
        <div><p className="eyebrow">Évaluation déterministe</p><h2>Indicateur Java de risque</h2><p>Résultat tel que retourné par le service de scoring.</p></div>
        <div className="admin-score-gauge"><strong>{percent.format(score)}</strong><span>/ 100</span></div>
        <div className="admin-score-meta"><span className={`admin-risk-label risk-${String(scoring.riskLevel).toLowerCase()}`}>Risque {riskText}</span><span>{scoring.provisional ? "Évaluation provisoire" : "Données disponibles"}</span><small>{scoring.evaluatedAt ? new Date(scoring.evaluatedAt).toLocaleString("fr-TN", { dateStyle: "medium", timeStyle: "short" }) : ""}</small></div>
      </section>
      {scoring.provisional && <p className="admin-comparison-warning">Scoring provisoire. Informations manquantes : {(scoring.missingInformation || []).join(", ") || "voir critères ci-dessous"}.</p>}
      <section className="card admin-detail-section">
        <SectionHeading eyebrow="Valeurs conservées du backend" title="Critères et justifications" icon={ShieldCheck} />
        <div className="admin-scoring-factors">
          {scoring.factors.map((factor) => {
            const points = Number(factor.points);
            const maximum = Number(factor.maximumPoints);
            const hasPoints = factor.points != null && factor.maximumPoints != null && Number.isFinite(points) && Number.isFinite(maximum);
            const progress = hasPoints && maximum > 0 ? Math.max(0, Math.min(100, (points / maximum) * 100)) : 0;
            return <article className={`admin-scoring-factor${factor.available ? "" : " unavailable"}`} key={factor.code}>
              <header><div><strong>{factor.label}</strong><small>{factor.inputValue || (factor.available ? "Donnée disponible" : "Donnée indisponible")}</small></div><b>{hasPoints ? `${factor.points}/${factor.maximumPoints}` : "—"}<small>{hasPoints ? "points" : ""}</small></b></header>
              <div className="admin-factor-track"><span style={{ width: `${progress}%` }} /></div>
              <p>{factor.explanation}</p>
            </article>;
          })}
        </div>
        <p className="admin-independence-note">{scoring.disclaimer}</p>
      </section>
    </div>
  );
}

function AnalysisTab({ application, busy, onEvaluateAi }) {
  const ai = application.aiEvaluation;
  const comparison = application.comparison;
  const reasons = [
    ["Motifs de l’analyse Gemini", ai?.reasons],
    ["Informations manquantes", ai?.missingInformation],
    ["Avertissements", ai?.warnings],
    ["Explications des écarts", comparison?.explanations],
  ];
  return (
    <div className="admin-tab-content">
      <section className="card admin-detail-section">
        <header className="admin-section-heading"><span className="admin-section-icon ai"><Sparkles size={18} /></span><div><p className="eyebrow">Analyses indépendantes</p><h2>Java et Gemini</h2></div>
          {application.status === "UNDER_REVIEW" && <button type="button" className="button button-small" disabled={busy} onClick={onEvaluateAi}><Sparkles size={15} />{busy ? "Analyse en cours…" : ai ? "Relancer l’analyse" : "Lancer l’analyse Gemini"}</button>}
        </header>
        {!ai ? <div className="admin-analysis-empty"><CircleHelp size={19} /><p>Aucune analyse Gemini n’est enregistrée pour cette demande.</p></div> : <>
          <div className={`admin-ai-status ${String(ai.status || "").toLowerCase()}`}>
            {ai.status === "SUCCESS" ? "Analyse Gemini reçue" : `Analyse : ${ai.status || "inconnue"}`}
            {ai.evaluatedAt && <small>{new Date(ai.evaluatedAt).toLocaleString("fr-TN", { dateStyle: "medium", timeStyle: "short" })}</small>}
          </div>
          {ai.message && <p className="admin-ai-message">{ai.message}</p>}
        </>}
        <div className="admin-analysis-columns">
          <article className="admin-analysis-card java"><span>Scoring Java</span><strong>{riskLabel(comparison?.javaRiskLevel ?? application.scoring?.riskLevel)}</strong><small>Éligibilité : {eligibilityLabel(comparison?.javaEligibilityStatus)}</small></article>
          <article className="admin-analysis-card gemini"><span>Analyse Gemini</span><strong>{riskLabel(comparison?.aiRiskLevel ?? ai?.riskLevel)}</strong><small>Éligibilité : {eligibilityLabel(comparison?.aiEligibilityStatus ?? ai?.eligibilityStatus)}</small></article>
        </div>
        <div className="admin-agreement-grid">
          <div><small>Concordance du risque</small><strong className={comparison?.riskAgreement === false ? "divergent" : ""}>{agreementLabel(comparison?.riskAgreement)}</strong></div>
          <div><small>Concordance de l’éligibilité</small><strong className={comparison?.eligibilityAgreement === false ? "divergent" : ""}>{agreementLabel(comparison?.eligibilityAgreement)}</strong></div>
        </div>
        {ai && <>
          {comparison?.scoringProvisional && <p className="admin-comparison-warning">Le scoring Java est provisoire : des données transactionnelles sont manquantes.</p>}
          {reasons.map(([title, items]) => items?.length > 0 && <section className="admin-explanation-list" key={title}><h3>{title}</h3><ul>{items.map((item, index) => <li key={`${title}-${index}`}>{item}</li>)}</ul></section>)}
        </>}
        <p className="admin-independence-note">Java et Gemini sont affichés séparément. Ces analyses assistent l’instruction ; elles ne prennent pas la décision de crédit.</p>
      </section>
    </div>
  );
}

function DecisionTab({ application, busy, note, decision, decisionReason, onNoteChange, onDecisionChange, onDecisionReasonChange, onClaim, onRequestInfo, onDecide }) {
  return (
    <div className="admin-decision-layout">
      <section className="card admin-human-decision">
        <p className="eyebrow">Contrôle humain</p><h2>Décision de l’administrateur</h2>
        <p>Les indicateurs Java et Gemini assistent l’instruction, sans se substituer à votre décision.</p>
        {application.status === "SUBMITTED" && <button className="button button-primary admin-action-full" type="button" disabled={busy} onClick={onClaim}><ClipboardCheck size={17} />{busy ? "Traitement…" : "Commencer l’examen"}</button>}
        {application.status === "UNDER_REVIEW" && <div className="admin-review-actions">
          <label>Complément à demander<textarea maxLength="1000" value={note} onChange={(event) => onNoteChange(event.target.value)} /></label>
          <button className="button button-quiet admin-action-full" type="button" disabled={busy || !note.trim()} onClick={onRequestInfo}>Demander un complément</button>
          <div className="admin-decision-divider" />
          <label>Décision humaine<select value={decision} onChange={(event) => onDecisionChange(event.target.value)}><option value="APPROVED">Approuver</option><option value="REJECTED">Refuser</option></select></label>
          <label>Motif de décision<textarea maxLength="1000" value={decisionReason} onChange={(event) => onDecisionReasonChange(event.target.value)} /></label>
          <button className="button button-primary admin-action-full" type="button" disabled={busy || !decisionReason.trim()} onClick={onDecide}><Check size={17} />Enregistrer la décision humaine</button>
        </div>}
        {!["SUBMITTED", "UNDER_REVIEW"].includes(application.status) && <div className="admin-locked-state">Aucune action de traitement n’est disponible pour ce statut.</div>}
      </section>
      <section className="card admin-history-card">
        <header className="admin-panel-heading"><div><p className="eyebrow">Traçabilité métier</p><h2>Historique du dossier</h2></div></header>
        {application.history?.length ? <ol className="admin-timeline">{application.history.map((event) => (
          <li key={`${event.createdAt}-${event.toStatus}`}><span className="admin-timeline-dot" /><div><strong>{applicationStatusLabel(event.toStatus)}</strong><small>{new Date(event.createdAt).toLocaleString("fr-TN", { dateStyle: "medium", timeStyle: "short" })}</small><small>{event.actorEmail}</small>{event.note && <p>{event.note}</p>}</div></li>
        ))}</ol> : <p className="admin-muted">Aucun événement d’historique disponible.</p>}
      </section>
    </div>
  );
}

export default function AdminApplicationDetail({
  application,
  busy,
  note,
  decision,
  decisionReason,
  onBack,
  onNoteChange,
  onDecisionChange,
  onDecisionReasonChange,
  onClaim,
  onRequestInfo,
  onDecide,
  onEvaluateAi,
}) {
  const [activeTab, setActiveTab] = useState("financial");
  return (
    <section className="admin-detail-page">
      <button className="admin-back-button" type="button" onClick={onBack}><ArrowLeft size={17} /> Retour aux demandes</button>
      <header className="admin-detail-header">
        <div><p className="eyebrow">{application.reference} · {scenarioLabel(application.scenario)}</p><h1>{application.customerName}</h1><p>{application.customerEmail} · créée le {new Date(application.createdAt).toLocaleDateString("fr-TN", { dateStyle: "medium" })}</p></div>
        <span className={`admin-status-badge status-${String(application.status || "unknown").toLowerCase()}`}>{applicationStatusLabel(application.status)}</span>
      </header>
      <section className="admin-detail-summary">
        <article><small>Montant demandé</small><strong>{application.requestedAmount == null ? "—" : currency.format(application.requestedAmount)}</strong><span>{application.durationMonths ?? "—"} mois</span></article>
        <article><small>Mensualité estimée</small><strong>{application.simulation?.monthlyPayment == null ? "—" : currency.format(application.simulation.monthlyPayment)}</strong><span>Résultat de simulation</span></article>
        <article><small>Score Java</small><strong>{application.scoring?.scoreOutOf100 == null ? "—" : `${percent.format(application.scoring.scoreOutOf100)} / 100`}</strong><span>Risque {riskLabel(application.scoring?.riskLevel)}</span></article>
      </section>
      <nav className="admin-detail-tabs" aria-label="Sections du dossier">
        {tabs.map(({ id, label, icon: Icon }) => <button key={id} className={activeTab === id ? "active" : ""} type="button" aria-current={activeTab === id ? "page" : undefined} onClick={() => setActiveTab(id)}><Icon size={17} />{label}</button>)}
      </nav>
      {activeTab === "financial" && <FinancialTab application={application} />}
      {activeTab === "scoring" && <ScoringTab scoring={application.scoring} />}
      {activeTab === "analysis" && <AnalysisTab application={application} busy={busy} onEvaluateAi={onEvaluateAi} />}
      {activeTab === "decision" && <DecisionTab application={application} busy={busy} note={note} decision={decision} decisionReason={decisionReason} onNoteChange={onNoteChange} onDecisionChange={onDecisionChange} onDecisionReasonChange={onDecisionReasonChange} onClaim={onClaim} onRequestInfo={onRequestInfo} onDecide={onDecide} />}
    </section>
  );
}
