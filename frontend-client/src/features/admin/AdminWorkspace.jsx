import { useCallback, useEffect, useState } from "react";
import api from "../../services/api";
import { applicationScenarioLabel, applicationStatusLabel } from "../applications/statusLabels";
import CreditScoringPanel from "../scoring/CreditScoringPanel";

const scenarios = [
  { value: "SANS_RACHAT", label: "Sans rachat" },
  { value: "RACHAT_INTERNE", label: "Rachat interne" },
  { value: "RACHAT_EXTERNE", label: "Rachat externe" },
];
const currency = new Intl.NumberFormat("fr-TN", { style: "currency", currency: "TND" });

function messageFrom(error) {
  return error.response?.data?.detail || error.response?.data?.error || error.response?.data?.message
    || "Impossible de contacter le service.";
}

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

export default function AdminWorkspace() {
  const [section, setSection] = useState("applications");
  const [parameters, setParameters] = useState(null);
  const [margins, setMargins] = useState({});
  const [tmm, setTmm] = useState("");
  const [applications, setApplications] = useState([]);
  const [selected, setSelected] = useState(null);
  const [note, setNote] = useState("");
  const [decision, setDecision] = useState("APPROVED");
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [noticeKind, setNoticeKind] = useState("error");

  const loadApplications = useCallback(async () => {
    const { data } = await api.get("/admin/applications");
    setApplications(data);
  }, []);

  const loadParameters = useCallback(async () => {
    const { data } = await api.get("/admin/parameters");
    setParameters(data);
    setTmm(data.tmmPercent ?? "");
    setMargins(Object.fromEntries(data.margins.map((item) => [item.id, {
      marginPercent: item.marginPercent,
      maxMonths: item.maxMonths,
    }])));
  }, []);

  useEffect(() => {
    loadApplications().catch((error) => setNotice(messageFrom(error)));
    loadParameters().catch((error) => setNotice(messageFrom(error)));
  }, [loadApplications, loadParameters]);

  async function openApplication(application) {
    try {
      const { data } = await api.get(`/admin/applications/${application.id}`);
      setSelected(data);
      setNote("");
    } catch (error) {
      setNotice(messageFrom(error));
    }
  }

  async function updateApplication(action, payload = {}) {
    if (!selected) return;
    setBusy(true);
    setNotice("");
    try {
      const { data } = await api.post(`/admin/applications/${selected.id}/${action}`, payload);
      setSelected(data);
      await loadApplications();
      setNote("");
      setNotice("Action enregistrée dans l’historique.");
      setNoticeKind("success");
    } catch (error) {
      setNotice(messageFrom(error));
      setNoticeKind("error");
    } finally {
      setBusy(false);
    }
  }

  async function evaluateAi() {
    if (!selected) return;
    setBusy(true);
    setNotice("");
    try {
      const { data } = await api.post(`/admin/applications/${selected.id}/ai-evaluation`);
      setSelected((current) => current ? { ...current, ...data } : current);
      await loadApplications();
      setNotice(data.aiEvaluation?.status === "SUCCESS"
        ? "Analyse IA enregistrée. Elle reste indépendante de la décision humaine."
        : `Analyse IA terminée avec le statut ${data.aiEvaluation?.status ?? "inconnu"} ; le scoring Java reste inchangé.`);
      setNoticeKind(data.aiEvaluation?.status === "SUCCESS" ? "success" : "error");
    } catch (error) {
      setNotice(messageFrom(error));
      setNoticeKind("error");
    } finally {
      setBusy(false);
    }
  }

  async function saveTmm(event) {
    event.preventDefault();
    setBusy(true);
    try {
      await api.put("/admin/parameters/tmm", { valuePercent: Number(tmm) });
      await loadParameters();
      setNotice("TMM mis à jour.");
      setNoticeKind("success");
    } catch (error) {
      setNotice(messageFrom(error));
      setNoticeKind("error");
    } finally {
      setBusy(false);
    }
  }

  async function saveMargin(item) {
    setBusy(true);
    try {
      await api.put("/admin/parameters/margins", {
        scenario: item.scenario,
        creditType: item.creditType,
        marginPercent: Number(margins[item.id].marginPercent),
        maxMonths: Number(margins[item.id].maxMonths),
      });
      await loadParameters();
      setNotice(`Paramètres enregistrés pour « ${item.creditType} ».`);
      setNoticeKind("success");
    } catch (error) {
      setNotice(messageFrom(error));
      setNoticeKind("error");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="admin-workspace">
      <div className="page-heading">
        <p className="eyebrow">Espace administrateur</p>
        <h1>{section === "applications" ? "Gestion des demandes de crédit" : "Paramètres de simulation"}</h1>
        <p>Les évaluations sont des aides à l’instruction. La décision de crédit reste humaine.</p>
      </div>
      <div className="tab-switch admin-workspace-tabs" role="tablist" aria-label="Fonctions d’administration">
        <button type="button" className={section === "applications" ? "active" : ""} onClick={() => setSection("applications")}>Demandes ({applications.length})</button>
        <button type="button" className={section === "parameters" ? "active" : ""} onClick={() => setSection("parameters")}>Paramètres du simulateur</button>
      </div>
      {notice && <p className={`notice ${noticeKind}`} role="status">{notice}</p>}

      {section === "applications" ? (
        <div className="admin-review-layout">
          <div className="card table-wrap">
            {applications.length === 0 ? <p className="empty-card">Aucune demande soumise à afficher.</p> : (
              <table>
                <thead><tr><th>Référence</th><th>Client</th><th>Produit / scénario</th><th>Montant</th><th>Durée</th><th>Statut</th><th>Action</th></tr></thead>
                <tbody>{applications.map((item) => (
                  <tr key={item.id}>
                    <td>{item.reference.slice(0, 8)}<br /><small>{item.submittedAt ? new Date(item.submittedAt).toLocaleDateString("fr-TN") : ""}</small></td>
                    <td>{item.customerName}<br /><small>{item.customerEmail}</small></td>
                    <td>{item.creditType || "Produit non renseigné"}<br /><small>{applicationScenarioLabel(item.scenario)}</small></td>
                    <td>{item.requestedAmount == null ? "—" : currency.format(item.requestedAmount)}</td>
                    <td>{item.durationMonths} mois</td>
                    <td>{applicationStatusLabel(item.status)}</td>
                    <td><button className="button button-small" onClick={() => openApplication(item)}>Détail</button></td>
                  </tr>
                ))}</tbody>
              </table>
            )}
          </div>
          {selected && (
            <article className="card admin-review-detail">
              <p className="eyebrow">Demande {selected.reference}</p>
              <h2>{selected.customerName}</h2>
              <p>{selected.customerEmail} · {applicationScenarioLabel(selected.scenario)}</p>
              <dl className="admin-facts">
                <div><dt>Produit demandé</dt><dd>{selected.creditType}</dd></div>
                <div><dt>Montant / durée</dt><dd>{selected.requestedAmount == null ? "—" : currency.format(selected.requestedAmount)} · {selected.durationMonths} mois</dd></div>
                <div><dt>Salaire moyen / loyer retenu</dt><dd>{currency.format(selected.averageMonthlySalary)} / {currency.format(selected.halfRentIncome)}</dd></div>
                <div><dt>Retenues SED / BNA</dt><dd>{currency.format(selected.sedDeductions)} / {currency.format(selected.bnaDeductions)}</dd></div>
                <div><dt>Capital restant à racheter</dt><dd>{currency.format(selected.remainingBuyoutCapital)}</dd></div>
                <div><dt>Revenu retenu (simulation)</dt><dd>{selected.simulation?.netIncome == null ? "—" : currency.format(selected.simulation.netIncome)}</dd></div>
                <div><dt>Taux / garde-fou</dt><dd>{selected.simulation?.debtRatePercent ?? "—"} % / {selected.simulation?.safeguardPercent ?? "—"} %</dd></div>
                <div><dt>Capacité mensuelle / max calculé</dt><dd>{selected.simulation?.monthlyCapacity == null ? "—" : currency.format(selected.simulation.monthlyCapacity)} / {selected.simulation?.maximumAmount == null ? "—" : currency.format(selected.simulation.maximumAmount)}</dd></div>
                <div><dt>Taux annuel / mensualité</dt><dd>{selected.simulation?.annualRatePercent ?? "—"} % / {selected.simulation?.monthlyPayment == null ? "—" : currency.format(selected.simulation.monthlyPayment)}</dd></div>
              </dl>
              <p className="admin-simulation-status">Simulation : {selected.simulation?.status ?? "Non disponible"}{selected.simulation?.message ? ` — ${selected.simulation.message}` : ""}</p>
              <CreditScoringPanel scoring={selected.scoring} />
              {selected.status === "UNDER_REVIEW" && (
                <section className="admin-ai-evaluation" aria-labelledby="ai-evaluation-title">
                  <div className="admin-ai-heading">
                    <div>
                      <h3 id="ai-evaluation-title">Analyse IA et comparaison</h3>
                      <p>Évaluation indicative indépendante ; elle ne prend aucune décision de crédit.</p>
                    </div>
                    <button type="button" className="button button-small" disabled={busy} onClick={evaluateAi}>
                      {busy ? "Analyse en cours…" : selected.aiEvaluation ? "Relancer l’analyse IA" : "Lancer l’analyse IA"}
                    </button>
                  </div>
                  {selected.aiEvaluation ? (
                    <>
                      <p className={`admin-ai-status ${selected.aiEvaluation.status.toLowerCase()}`}>
                        {selected.aiEvaluation.status === "SUCCESS" ? "Analyse reçue" : `Analyse : ${selected.aiEvaluation.status}`}
                        {selected.aiEvaluation.evaluatedAt ? ` · ${new Date(selected.aiEvaluation.evaluatedAt).toLocaleString("fr-TN")}` : ""}
                      </p>
                      {selected.aiEvaluation.message && <p>{selected.aiEvaluation.message}</p>}
                      <dl className="admin-ai-comparison">
                        <div><dt>Éligibilité Java</dt><dd>{eligibilityLabel(selected.comparison?.javaEligibilityStatus)}</dd></div>
                        <div><dt>Éligibilité IA proposée</dt><dd>{eligibilityLabel(selected.comparison?.aiEligibilityStatus ?? selected.aiEvaluation.eligibilityStatus)}</dd></div>
                        <div><dt>Comparaison éligibilité</dt><dd>{agreementLabel(selected.comparison?.eligibilityAgreement)}</dd></div>
                        <div><dt>Risque Java</dt><dd>{riskLabel(selected.comparison?.javaRiskLevel ?? selected.scoring?.riskLevel)}</dd></div>
                        <div><dt>Risque IA proposé</dt><dd>{riskLabel(selected.comparison?.aiRiskLevel ?? selected.aiEvaluation.riskLevel)}</dd></div>
                        <div><dt>Comparaison risque</dt><dd>{agreementLabel(selected.comparison?.riskAgreement)}</dd></div>
                      </dl>
                      {selected.comparison?.scoringProvisional && <p className="admin-ai-warning">Le scoring Java est provisoire : des données transactionnelles sont manquantes.</p>}
                      {[
                        ["Motifs de l’analyse IA", selected.aiEvaluation.reasons],
                        ["Informations manquantes", selected.aiEvaluation.missingInformation],
                        ["Avertissements", selected.aiEvaluation.warnings],
                        ["Explications des écarts", selected.comparison?.explanations],
                      ].map(([title, items]) => items?.length > 0 && (
                        <div className="admin-ai-list" key={title}>
                          <strong>{title}</strong>
                          <ul>{items.map((item, index) => <li key={`${title}-${index}`}>{item}</li>)}</ul>
                        </div>
                      ))}
                    </>
                  ) : <p className="admin-ai-empty">Aucune analyse IA n’a encore été exécutée pour cette demande.</p>}
                </section>
              )}
              {selected.status === "SUBMITTED" && <button className="button button-primary" disabled={busy} onClick={() => updateApplication("claim")}>Commencer l’examen</button>}
              {selected.status === "UNDER_REVIEW" && (
                <div className="admin-review-actions">
                  <label>Complément à demander
                    <textarea maxLength="1000" value={note} onChange={(event) => setNote(event.target.value)} />
                  </label>
                  <button className="button" disabled={busy || !note.trim()} onClick={() => updateApplication("request-info", { note })}>Demander un complément</button>
                  <label>Décision humaine
                    <select value={decision} onChange={(event) => setDecision(event.target.value)}>
                      <option value="APPROVED">Accepter</option><option value="REJECTED">Refuser</option>
                    </select>
                  </label>
                  <label>Motif de décision
                    <textarea maxLength="1000" value={note} onChange={(event) => setNote(event.target.value)} />
                  </label>
                  <button className="button button-primary" disabled={busy || !note.trim()} onClick={() => updateApplication("decision", { decision, reason: note })}>Enregistrer la décision</button>
                </div>
              )}
              <details className="application-history" open>
                <summary>Historique</summary>
                {selected.history.map((event) => <p key={`${event.createdAt}-${event.toStatus}`}>{new Date(event.createdAt).toLocaleString("fr-TN")} · {applicationStatusLabel(event.toStatus)} · {event.actorEmail}{event.note ? ` — ${event.note}` : ""}</p>)}
              </details>
            </article>
          )}
        </div>
      ) : (
        <div className="admin-parameters">
          {!parameters ? <div className="card empty-card">Chargement des paramètres…</div> : <>
            <form className="card admin-tmm" onSubmit={saveTmm}>
              <label>TMM annuel (%)
                <input type="number" required min="0" max="100" step="0.0001" value={tmm} onChange={(event) => setTmm(event.target.value)} />
              </label>
              <button className="button button-primary" disabled={busy}>Enregistrer</button>
            </form>
            {scenarios.map((scenario) => {
              const rows = parameters.margins.filter((item) => item.scenario === scenario.value);
              return <section className="parameter-section" key={scenario.value}>
                <div className="section-heading"><h2>{scenario.label}</h2><span className="count-badge">{rows.length} produits</span></div>
                <div className="card table-wrap"><table><thead><tr><th>Produit</th><th>Marge (%)</th><th>Durée maximale</th><th></th></tr></thead>
                  <tbody>{rows.map((item) => <tr key={item.id}>
                    <td>{item.creditType}</td>
                    <td><input className="table-input" type="number" min="0" max="100" step="0.0001" value={margins[item.id]?.marginPercent ?? ""} onChange={(event) => setMargins((current) => ({ ...current, [item.id]: { ...current[item.id], marginPercent: event.target.value } }))} /></td>
                    <td><input className="table-input" type="number" min="1" max="1200" value={margins[item.id]?.maxMonths ?? ""} onChange={(event) => setMargins((current) => ({ ...current, [item.id]: { ...current[item.id], maxMonths: event.target.value } }))} /></td>
                    <td><button className="button button-small" disabled={busy} onClick={() => saveMargin(item)}>Enregistrer</button></td>
                  </tr>)}</tbody>
                </table></div>
              </section>;
            })}
          </>}
        </div>
      )}
    </section>
  );
}
