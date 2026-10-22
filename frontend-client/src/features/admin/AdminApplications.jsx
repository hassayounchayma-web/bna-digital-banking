import { useMemo, useState } from "react";
import { ArrowRight, Search, SlidersHorizontal } from "lucide-react";
import { applicationScenarioLabel, applicationStatusLabel } from "../applications/statusLabels";

const currency = new Intl.NumberFormat("fr-TN", {
  style: "currency",
  currency: "TND",
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});
const pageSize = 10;
const statuses = ["DRAFT", "SUBMITTED", "UNDER_REVIEW", "ADDITIONAL_INFO_REQUIRED", "APPROVED", "REJECTED", "WITHDRAWN"];
const riskLevels = ["LOW", "MODERATE", "HIGH"];
const scenarios = ["SANS_RACHAT", "RACHAT_INTERNE", "RACHAT_EXTERNE"];

function dateValue(application) {
  return application.submittedAt || application.createdAt || application.updatedAt;
}

function scenarioLabel(scenario) {
  return ({
    SANS_RACHAT: "Sans rachat",
    RACHAT_INTERNE: "Rachat interne",
    RACHAT_EXTERNE: "Rachat externe",
  })[scenario] || applicationScenarioLabel(scenario);
}

function StatusBadge({ status }) {
  return <span className={`admin-status-badge status-${String(status || "unknown").toLowerCase()}`}>{applicationStatusLabel(status)}</span>;
}

export default function AdminApplications({ applications, loading, onOpen }) {
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("ALL");
  const [scenario, setScenario] = useState("ALL");
  const [creditType, setCreditType] = useState("ALL");
  const [risk, setRisk] = useState("ALL");
  const [page, setPage] = useState(1);
  const products = [...new Set(applications.map((application) => application.creditType).filter(Boolean))].sort((a, b) => a.localeCompare(b, "fr"));

  const filtered = useMemo(() => {
    const normalizedQuery = query.trim().toLocaleLowerCase("fr");
    return [...applications]
      .filter((application) => status === "ALL" || application.status === status)
      .filter((application) => scenario === "ALL" || application.scenario === scenario)
      .filter((application) => creditType === "ALL" || application.creditType === creditType)
      .filter((application) => risk === "ALL" || application.scoring?.riskLevel === risk)
      .filter((application) => !normalizedQuery || [
        application.reference,
        application.customerName,
        application.customerEmail,
        application.creditType,
        scenarioLabel(application.scenario),
      ].some((value) => String(value || "").toLocaleLowerCase("fr").includes(normalizedQuery)))
      .sort((left, right) => new Date(dateValue(right) || 0) - new Date(dateValue(left) || 0));
  }, [applications, creditType, query, risk, scenario, status]);

  const pageCount = Math.max(1, Math.ceil(filtered.length / pageSize));
  const pageItems = filtered.slice((page - 1) * pageSize, page * pageSize);

  function updateFilter(setter, value) {
    setter(value);
    setPage(1);
  }

  return (
    <section className="admin-applications-page">
      <header className="admin-page-intro">
        <div><p className="eyebrow">Instruction des dossiers</p><h1>Demandes de crédit</h1><p>Recherchez et filtrez les demandes retournées par le service d’administration.</p></div>
        <span className="admin-data-chip">{filtered.length} dossier{filtered.length === 1 ? "" : "s"}</span>
      </header>
      <section className="card admin-registry">
        <header className="admin-registry-toolbar">
          <div className="admin-search">
            <Search size={17} aria-hidden="true" />
            <input aria-label="Rechercher dans les demandes" placeholder="Référence, client, e-mail…" value={query} onChange={(event) => updateFilter(setQuery, event.target.value)} />
          </div>
          <span className="admin-filter-label"><SlidersHorizontal size={16} /> Filtres</span>
          <select aria-label="Filtrer par statut" value={status} onChange={(event) => updateFilter(setStatus, event.target.value)}>
            <option value="ALL">Tous les statuts</option>
            {statuses.map((item) => <option value={item} key={item}>{applicationStatusLabel(item)}</option>)}
          </select>
          <select aria-label="Filtrer par scénario" value={scenario} onChange={(event) => updateFilter(setScenario, event.target.value)}>
            <option value="ALL">Tous les scénarios</option>
            {scenarios.map((item) => <option value={item} key={item}>{applicationScenarioLabel(item)}</option>)}
          </select>
          <select aria-label="Filtrer par produit" value={creditType} onChange={(event) => updateFilter(setCreditType, event.target.value)}>
            <option value="ALL">Tous les produits</option>
            {products.map((product) => <option value={product} key={product}>{product}</option>)}
          </select>
          <select aria-label="Filtrer par risque Java" value={risk} onChange={(event) => updateFilter(setRisk, event.target.value)}>
            <option value="ALL">Tous les risques Java</option>
            {riskLevels.map((level) => <option value={level} key={level}>{level}</option>)}
          </select>
        </header>
        {loading ? <div className="admin-empty-state">Chargement des demandes…</div> : pageItems.length === 0 ? (
          <div className="admin-empty-state"><Search size={22} /><p>{applications.length ? "Aucune demande ne correspond à ces critères." : "Aucune demande disponible."}</p></div>
        ) : (
          <>
            <div className="admin-table-wrap">
              <table className="admin-table admin-registry-table">
                <thead><tr><th>Demande</th><th>Client</th><th>Produit / scénario</th><th>Montant / durée</th><th>Risque Java</th><th>Statut</th><th /></tr></thead>
                <tbody>{pageItems.map((application) => (
                  <tr key={application.id}>
                    <td><strong>{application.reference}</strong><small>{dateValue(application) ? new Date(dateValue(application)).toLocaleDateString("fr-TN", { dateStyle: "medium" }) : "Date indisponible"}</small></td>
                    <td><strong>{application.customerName}</strong><small>{application.customerEmail}</small></td>
                    <td><strong>{application.creditType || "Produit non renseigné"}</strong><small>{scenarioLabel(application.scenario)}</small></td>
                    <td><strong>{application.requestedAmount == null ? "Montant à préciser" : currency.format(application.requestedAmount)}</strong><small>{application.durationMonths ?? "—"} mois</small></td>
                    <td>{application.scoring?.riskLevel ? <span className={`admin-risk-label risk-${application.scoring.riskLevel.toLowerCase()}`}>{application.scoring.riskLevel}</span> : <span className="admin-muted">Non disponible</span>}</td>
                    <td><StatusBadge status={application.status} /></td>
                    <td><button type="button" className="admin-open-button" onClick={() => onOpen(application)}>Ouvrir <ArrowRight size={15} /></button></td>
                  </tr>
                ))}</tbody>
              </table>
            </div>
            {pageCount > 1 && <footer className="admin-pagination">
              <span>{(page - 1) * pageSize + 1}–{Math.min(page * pageSize, filtered.length)} sur {filtered.length}</span>
              <div>
                <button type="button" disabled={page === 1} onClick={() => setPage((current) => current - 1)}>Précédent</button>
                <span>Page {page} / {pageCount}</span>
                <button type="button" disabled={page === pageCount} onClick={() => setPage((current) => current + 1)}>Suivant</button>
              </div>
            </footer>}
          </>
        )}
      </section>
    </section>
  );
}
