import { useState } from "react";
import { applicationScenarioLabel, applicationStatusLabel } from "./statusLabels";
import CreditScoringPanel from "../scoring/CreditScoringPanel";

const currency = new Intl.NumberFormat("fr-TN", { style: "currency", currency: "TND" });

export default function ClientApplications({
  applications,
  busy,
  onEditDraft,
  onResumeDraft,
  onProvideInfo,
  onWithdraw,
}) {
  const [notes, setNotes] = useState({});

  return (
    <section className="application-list">
      {applications.length === 0 ? <div className="card empty-result">Vous n’avez pas encore de demande de crédit.</div> : applications.map((application) => (
        <article className="card application-card" key={application.id}>
          <div className="application-heading">
            <div><p className="eyebrow">{application.reference?.slice(0, 8) ?? "Référence indisponible"} · {applicationScenarioLabel(application.scenario)}</p><h2>{application.creditType || "Produit non renseigné"}</h2></div>
            <span className={`result-status ${application.status === "APPROVED" ? "success" : "warning"}`}>{applicationStatusLabel(application.status)}</span>
          </div>
          <p>{application.requestedAmount ? currency.format(application.requestedAmount) : "Montant à préciser"} · {application.durationMonths} mois · Mis à jour le {new Date(application.updatedAt).toLocaleDateString("fr-TN")}</p>
          <CreditScoringPanel scoring={application.scoring} />
          {application.status === "DRAFT" && <div className="application-actions">
            <button className="button button-quiet" disabled={busy} onClick={() => onEditDraft(application)}>Modifier le brouillon</button>
            <button className="button button-primary" disabled={busy} onClick={() => onResumeDraft(application)}>Vérifier et soumettre</button>
          </div>}
          {application.history?.filter((event) => event.note && event.toStatus === "ADDITIONAL_INFO_REQUIRED").map((event) => (
            <p className="notice" key={event.createdAt}>Complément demandé : {event.note}</p>
          ))}
          {application.status === "ADDITIONAL_INFO_REQUIRED" && <div className="application-actions">
            <label>Votre complément
              <textarea maxLength="1000" required value={notes[application.id] || ""} onChange={(event) => setNotes((current) => ({ ...current, [application.id]: event.target.value }))} />
            </label>
            <button className="button button-primary" disabled={busy || !notes[application.id]?.trim()} onClick={async () => {
              const provided = await onProvideInfo(application, notes[application.id]);
              if (provided) setNotes((current) => ({ ...current, [application.id]: "" }));
            }}>Transmettre le complément</button>
          </div>}
          {["DRAFT", "SUBMITTED", "UNDER_REVIEW", "ADDITIONAL_INFO_REQUIRED"].includes(application.status) && (
            <button className="button button-quiet" disabled={busy} onClick={() => onWithdraw(application)}>Retirer la demande</button>
          )}
          {application.history?.length > 0 && <details className="application-history">
            <summary>Historique de la demande</summary>
            {application.history.map((event) => <p key={`${event.createdAt}-${event.toStatus}`}>{new Date(event.createdAt).toLocaleString("fr-TN")} · {applicationStatusLabel(event.toStatus)} · {event.actorRole === "ADMIN" ? "Administration" : "Vous"}{event.note ? ` — ${event.note}` : ""}</p>)}
          </details>}
        </article>
      ))}
    </section>
  );
}
