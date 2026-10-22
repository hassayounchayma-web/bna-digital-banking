import {
  ArrowRight,
  CheckCheck,
  Clock3,
  FileClock,
  FileText,
  ShieldAlert,
  XCircle,
} from "lucide-react";
import { applicationScenarioLabel, applicationStatusLabel } from "../applications/statusLabels";

const currency = new Intl.NumberFormat("fr-TN", {
  style: "currency",
  currency: "TND",
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});

const statusCards = [
  { status: "SUBMITTED", label: "En attente", icon: FileClock, style: "pending" },
  { status: "UNDER_REVIEW", label: "En traitement", icon: Clock3, style: "review" },
  { status: "APPROVED", label: "Approuvées", icon: CheckCheck, style: "approved" },
  { status: "REJECTED", label: "Refusées", icon: XCircle, style: "rejected" },
];
const knownStatuses = new Set(statusCards.map((item) => item.status));

function applicationDate(application) {
  return application.submittedAt || application.createdAt || application.updatedAt;
}

function scenarioLabel(scenario) {
  return ({
    SANS_RACHAT: "Sans rachat",
    RACHAT_INTERNE: "Rachat interne",
    RACHAT_EXTERNE: "Rachat externe",
  })[scenario] || applicationScenarioLabel(scenario);
}

function formatDate(value) {
  if (!value) return "—";
  return new Date(value).toLocaleDateString("fr-TN", { dateStyle: "medium" });
}

function StatusBadge({ status }) {
  return <span className={`admin-status-badge status-${String(status || "unknown").toLowerCase()}`}>{applicationStatusLabel(status)}</span>;
}

export default function AdminDashboard({ applications, loading, onOpen, onNavigate }) {
  const count = (status) => applications.filter((application) => application.status === status).length;
  const scored = applications.filter((application) => application.scoring?.riskLevel);
  const risks = [...new Set(scored.map((application) => application.scoring.riskLevel))];
  const riskCounts = risks.map((level) => ({
    level,
    label: ({ LOW: "Faible", MODERATE: "Modéré", HIGH: "Élevé" })[level] || level,
    tone: ({ LOW: "low", MODERATE: "moderate", HIGH: "high" })[level] || "other",
    count: scored.filter((application) => application.scoring.riskLevel === level).length,
  }));
  const otherStatuses = applications.filter((application) => !knownStatuses.has(application.status));
  const recent = [...applications]
    .sort((left, right) => new Date(applicationDate(right) || 0) - new Date(applicationDate(left) || 0))
    .slice(0, 6);
  const queue = applications.filter((application) => application.status === "SUBMITTED" || application.status === "UNDER_REVIEW")
    .slice(0, 4);

  return (
    <div className="admin-dashboard">
      <section className="admin-welcome">
        <div>
          <p className="eyebrow">Pilotage du portefeuille crédit</p>
          <h1>Vue d’ensemble</h1>
          <p>Suivez les demandes et accédez aux dossiers nécessitant un traitement.</p>
        </div>
        <span className="admin-live-indicator"><i /> Données du backend</span>
      </section>

      <section className="admin-stat-grid" aria-label="Indicateurs des demandes">
        <article className="admin-stat-card total">
          <span className="admin-stat-icon"><FileText size={19} /></span>
          <small>Total des demandes</small>
          <strong>{loading ? "—" : applications.length}</strong>
          <span className="admin-stat-foot">Dossiers retournés par le service</span>
        </article>
        {statusCards.map(({ status, label, icon: Icon, style }) => (
          <article className="admin-stat-card" key={status}>
            <span className={`admin-stat-icon ${style}`}><Icon size={19} /></span>
            <small>{label}</small>
            <strong>{loading ? "—" : count(status)}</strong>
            <span className="admin-stat-foot">{applicationStatusLabel(status)}</span>
          </article>
        ))}
        <article className="admin-stat-card">
          <span className="admin-stat-icon other"><FileText size={19} /></span>
          <small>Autres statuts</small>
          <strong>{loading ? "—" : otherStatuses.length}</strong>
          <span className="admin-stat-foot">{[...new Set(otherStatuses.map((application) => applicationStatusLabel(application.status)))].join(" · ") || "Aucun autre statut"}</span>
        </article>
      </section>

      <section className="admin-dashboard-grid">
        <article className="card admin-panel admin-risk-panel">
          <header className="admin-panel-heading">
            <div><p className="eyebrow">Indicateur Java</p><h2>Répartition des risques</h2></div>
            <span className="admin-data-chip">{scored.length} évalué{scored.length === 1 ? "" : "s"}</span>
          </header>
          {scored.length === 0 ? (
            <div className="admin-empty-state"><ShieldAlert size={22} /><p>Aucune évaluation Java disponible dans les demandes chargées.</p></div>
          ) : (
            <div className="admin-risk-list">
              {riskCounts.map((risk) => (
                <div className="admin-risk-row" key={risk.level}>
                  <span className={`admin-risk-dot ${risk.tone}`} />
                  <span>{risk.label}</span>
                  <div className="admin-risk-track"><i className={risk.tone} style={{ width: `${(risk.count / scored.length) * 100}%` }} /></div>
                  <strong>{risk.count}</strong>
                </div>
              ))}
              <p className="admin-footnote">Répartition calculée uniquement sur les demandes ayant un scoring Java avec un niveau de risque renseigné.</p>
            </div>
          )}
        </article>

        <article className="card admin-panel admin-queue-panel">
          <header className="admin-panel-heading">
            <div><p className="eyebrow">À traiter</p><h2>File de traitement</h2></div>
            <button className="admin-text-link" type="button" onClick={() => onNavigate("applications")}>Toutes les demandes <ArrowRight size={15} /></button>
          </header>
          {queue.length === 0 ? <div className="admin-empty-state"><CheckCheck size={22} /><p>Aucun dossier en attente ou en examen parmi les demandes chargées.</p></div> : (
            <div className="admin-queue-list">
              {queue.map((application) => (
                <button type="button" key={application.id} onClick={() => onOpen(application)}>
                  <span className="admin-queue-avatar">{String(application.customerName || "?").slice(0, 1).toUpperCase()}</span>
                  <span className="admin-queue-copy"><strong>{application.customerName}</strong><small>{application.reference} · {application.creditType || scenarioLabel(application.scenario)}</small></span>
                  <StatusBadge status={application.status} />
                  <ArrowRight size={16} aria-hidden="true" />
                </button>
              ))}
            </div>
          )}
        </article>
      </section>

      <section className="card admin-panel admin-recent-panel">
        <header className="admin-panel-heading">
          <div><p className="eyebrow">Activité récente</p><h2>Demandes récentes</h2></div>
          <button className="admin-text-link" type="button" onClick={() => onNavigate("applications")}>Ouvrir le registre <ArrowRight size={15} /></button>
        </header>
        {loading ? <div className="admin-empty-state">Chargement des demandes…</div> : recent.length === 0 ? (
          <div className="admin-empty-state"><FileText size={22} /><p>Aucune demande disponible.</p></div>
        ) : (
          <div className="admin-table-wrap">
            <table className="admin-table">
              <thead><tr><th>Référence / date</th><th>Client</th><th>Produit / scénario</th><th>Montant</th><th>Risque Java</th><th>Statut</th><th /></tr></thead>
              <tbody>{recent.map((application) => (
                <tr key={application.id}>
                  <td><strong>{application.reference}</strong><small>{formatDate(applicationDate(application))}</small></td>
                  <td><strong>{application.customerName}</strong><small>{application.customerEmail}</small></td>
                  <td><strong>{application.creditType || "Produit non renseigné"}</strong><small>{applicationScenarioLabel(application.scenario)}</small></td>
                  <td>{application.requestedAmount == null ? "—" : currency.format(application.requestedAmount)}</td>
                  <td>{application.scoring?.riskLevel ? <span className={`admin-risk-label risk-${application.scoring.riskLevel.toLowerCase()}`}>{application.scoring.riskLevel}</span> : "—"}</td>
                  <td><StatusBadge status={application.status} /></td>
                  <td><button className="admin-row-action" type="button" onClick={() => onOpen(application)} aria-label={`Ouvrir la demande ${application.reference}`}><ArrowRight size={17} /></button></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
