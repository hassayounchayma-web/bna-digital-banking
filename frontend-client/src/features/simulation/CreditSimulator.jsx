const currency = new Intl.NumberFormat("fr-TN", { style: "currency", currency: "TND" });
const percent = new Intl.NumberFormat("fr-TN", { maximumFractionDigits: 2 });

export default function CreditSimulator({
  scenarios,
  scenario,
  onScenarioChange,
  catalogue,
  values,
  onValueChange,
  onProductChange,
  isBuyout,
  busy,
  result,
  onSimulate,
  onSaveDraft,
  onBeginConfirmation,
  onConfirmSubmission,
  confirming,
  editing,
}) {
  const products = Object.entries(catalogue);
  const selectedProduct = catalogue[values.typeCredit];
  const maxMonths = Number.isInteger(selectedProduct?.maxMonths) && selectedProduct.maxMonths > 0
    ? selectedProduct.maxMonths
    : 0;

  return (
    <>
      <div className="application-progress" aria-label="Étapes de la demande">
        <span className="active"><b>1</b> Projet et simulation</span>
        <span className={result ? "active" : ""}><b>2</b> Résultat et vérification</span>
        <span className={confirming ? "active" : ""}><b>3</b> Confirmation</span>
      </div>
      <div className="simulation-grid">
        <form className="card simulation-form" onSubmit={onSimulate}>
          <h2>Demander un crédit</h2>
          <label>Scénario de financement
            <select value={scenario} onChange={onScenarioChange}>
              {scenarios.map((item) => <option key={item.value} value={item.value}>{item.label}</option>)}
            </select>
          </label>
          <label>Produit de crédit
            <select name="typeCredit" required value={values.typeCredit || ""} onChange={onProductChange} disabled={products.length === 0}>
              {products.length === 0 && <option value="">Aucun produit disponible</option>}
              {products.map(([name]) => <option key={name} value={name}>{name}</option>)}
            </select>
          </label>
          {selectedProduct && <p className="field-hint">Durée maximale pour ce produit et ce scénario : {maxMonths} mois · Marge configurée : {percent.format(selectedProduct.annualMarginPercent * 100)} %</p>}

          <div className="field-grid">
            <label>Salaire net moyen mensuel (DT)
              <input name="salaireMensuelMoyen" type="number" required min="0" step="0.01" value={values.salaireMensuelMoyen} onChange={onValueChange} />
            </label>
            <label>Loyer mensuel (DT)
              <input name="loyerMensuel" type="number" min="0" step="0.01" value={values.loyerMensuel} onChange={onValueChange} />
            </label>
          </div>
          <p className="field-hint">Le revenu net moyen est calculé par l’API à partir du salaire et de 50 % du loyer.</p>

          <div className="field-grid">
            <label>Retenues SED (DT)
              <input name="retenuesSed" type="number" min="0" step="0.01" value={values.retenuesSed} onChange={onValueChange} />
            </label>
            {scenario === "SANS_RACHAT" && <label>Retenues des crédits BNA (DT)
              <input name="retenuesCreditsBna" type="number" min="0" step="0.01" value={values.retenuesCreditsBna} onChange={onValueChange} />
            </label>}
            {scenario === "RACHAT_INTERNE" && <label>Retenues BNA des crédits non rachetés (DT)
              <input name="retenuesCreditsBnaNonRachete" type="number" min="0" step="0.01" value={values.retenuesCreditsBnaNonRachete} onChange={onValueChange} />
            </label>}
          </div>
          {isBuyout && <label>Capital restant dû à racheter (DT)
            <input name="capitalRestantARacheter" type="number" required min="0.01" step="0.01" value={values.capitalRestantARacheter} onChange={onValueChange} />
          </label>}
          <div className="field-grid">
            <label>Durée (mois)
              <select key={`${scenario}:${values.typeCredit}:${maxMonths}`} name="dureeMois" required value={values.dureeMois} onChange={onValueChange} disabled={maxMonths === 0}>
                {Array.from({ length: maxMonths }, (_, index) => index + 1).map((months) => <option key={months} value={months}>{months} mois</option>)}
              </select>
            </label>
            <label>Montant demandé (DT)
              <input name="montantCreditDemande" type="number" min="0.01" step="0.01" value={values.montantCreditDemande} onChange={onValueChange} />
            </label>
          </div>
          <p className="field-hint scoring-evidence-note">
            L’indicateur de risque est calculé par le serveur à partir des opérations enregistrées.
          </p>
          <button className="button button-primary button-full" disabled={busy || products.length === 0}>{busy ? "Calcul en cours…" : "Calculer ma simulation"}</button>
          {result && <button type="button" className="button button-quiet button-full" disabled={busy} onClick={onSaveDraft}>{editing ? "Enregistrer les modifications du brouillon" : "Enregistrer en brouillon"}</button>}
        </form>

        <aside className="card result-card" aria-live="polite">
          <p className="eyebrow">Résultat calculé par le backend</p>
          {!result ? <div className="empty-result">
            <div className="result-mark">↗</div><h2>Votre estimation apparaîtra ici</h2>
            <p>Complétez les informations puis lancez la simulation.</p>
          </div> : <>
            <span className={`result-status ${result.statut === "ELIGIBLE" ? "success" : "warning"}`}>{result.statut?.replaceAll("_", " ")}</span>
            {result.error && <p className="result-error">{result.error}</p>}
            <dl className="result-list">
              <div><dt>Revenu retenu</dt><dd>{result.totalRevenuNetMoyen == null ? "—" : currency.format(result.totalRevenuNetMoyen)}</dd></div>
              <div><dt>Taux / garde-fou</dt><dd>{result.tauxEndettement == null ? "—" : `${percent.format(result.tauxEndettement)} %`} / {result.gf == null ? "—" : `${percent.format(result.gf)} %`}</dd></div>
              <div><dt>Capacité mensuelle disponible</dt><dd>{result.availableMensualite == null ? "—" : currency.format(result.availableMensualite)}</dd></div>
              <div><dt>Taux annuel applicable</dt><dd>{result.tauxAnnuel == null ? "—" : `${percent.format(result.tauxAnnuel)} %`}</dd></div>
              <div><dt>Durée / montant demandé</dt><dd>{values.dureeMois} mois / {values.montantCreditDemande ? currency.format(Number(values.montantCreditDemande)) : "Non renseigné"}</dd></div>
              <div><dt>Montant maximal calculé</dt><dd>{result.montantMaxCredit == null ? "—" : currency.format(result.montantMaxCredit)}</dd></div>
              <div><dt>Mensualité estimée</dt><dd>{result.mensualite == null ? "—" : currency.format(result.mensualite)}</dd></div>
            </dl>
            {result.tauxEndettementCalcule != null && <p className="field-hint">Taux après simulation : {percent.format(result.tauxEndettementCalcule)} %</p>}
            {result.statut === "ELIGIBLE" && values.montantCreditDemande && !confirming && <button className="button button-primary button-full" onClick={onBeginConfirmation}>Vérifier ma demande</button>}
            {confirming && <div className="confirmation-panel">
              <h2>Vérifiez avant de confirmer</h2>
              <p>{scenarios.find((item) => item.value === scenario)?.label} · {values.typeCredit}</p>
              <p>Montant : {currency.format(Number(values.montantCreditDemande))} · Durée : {values.dureeMois} mois</p>
              <p>Salaire mensuel moyen : {currency.format(Number(values.salaireMensuelMoyen))} · Loyer : {currency.format(Number(values.loyerMensuel || 0))}</p>
              <p className="field-hint">En confirmant, vous autorisez l’enregistrement de cette demande et de son instantané de simulation.</p>
              <button className="button button-primary button-full" disabled={busy} onClick={onConfirmSubmission}>{busy ? "Soumission…" : "Confirmer et soumettre la demande"}</button>
            </div>}
          </>}
          <div className="disclaimer">Simulation indicative : elle ne constitue pas une offre et ne prend aucune décision de crédit.</div>
        </aside>
      </div>
    </>
  );
}
