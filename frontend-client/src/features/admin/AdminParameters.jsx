import { useState } from "react";
import { Check, RotateCcw, Save, Settings2 } from "lucide-react";

const scenarios = [
  { value: "SANS_RACHAT", label: "Sans rachat" },
  { value: "RACHAT_INTERNE", label: "Rachat interne" },
  { value: "RACHAT_EXTERNE", label: "Rachat externe" },
];

export default function AdminParameters({
  parameters,
  loading,
  tmm,
  margins,
  dirtyMargins,
  tmmDirty,
  busy,
  onTmmChange,
  onMarginChange,
  onSaveTmm,
  onSaveMargin,
}) {
  const [savedRows, setSavedRows] = useState({});
  const [activeScenario, setActiveScenario] = useState(scenarios[0].value);

  async function saveMargin(event, item) {
    event.preventDefault();
    if (!window.confirm(`Confirmer l’enregistrement des paramètres de « ${item.creditType} » ?`)) return;
    const saved = await onSaveMargin(item);
    if (saved) setSavedRows((current) => ({ ...current, [item.id]: true }));
  }

  function changeMargin(id, field, value) {
    setSavedRows((current) => ({ ...current, [id]: false }));
    onMarginChange(id, field, value);
  }

  function saveTmm(event) {
    event.preventDefault();
    if (tmmDirty && window.confirm("Confirmer la mise à jour du TMM ?")) onSaveTmm(event);
  }

  return (
    <section className="admin-parameters-page">
      <header className="admin-page-intro">
        <div><p className="eyebrow">Configuration métier existante</p><h1>Paramètres du simulateur</h1><p>Gérez le TMM, les marges et les durées maximales configurés pour les produits.</p></div>
      </header>
      {loading && !parameters ? <div className="card admin-loading-panel">Chargement des paramètres du simulateur…</div> : parameters ? <>
        <section className="card admin-tmm-panel">
          <div className="admin-parameter-icon"><Settings2 size={20} /></div>
          <div className="admin-tmm-copy"><p className="eyebrow">Référence des taux</p><h2>Taux du marché monétaire (TMM)</h2><p>Valeur actuelle : <strong>{parameters.tmmPercent} %</strong> · référence annuelle du simulateur.</p></div>
          <form className="admin-tmm-form" onSubmit={saveTmm}>
            <label>Valeur annuelle (%)
              <input type="number" required min="0" max="100" step="0.0001" value={tmm} onChange={(event) => onTmmChange(event.target.value)} />
            </label>
            <button className="button button-primary" disabled={busy || !tmmDirty}><Save size={16} /> Enregistrer le TMM</button>
          </form>
        </section>

        <nav className="admin-scenario-tabs" aria-label="Filtrer par scénario">
          {scenarios.map((scenario) => <button type="button" key={scenario.value} className={activeScenario === scenario.value ? "active" : ""} aria-current={activeScenario === scenario.value ? "page" : undefined} onClick={() => setActiveScenario(scenario.value)}>
            {scenario.label}<span>{parameters.margins.filter((item) => item.scenario === scenario.value).length}</span>
          </button>)}
        </nav>

        {scenarios.filter((item) => item.value === activeScenario).map((scenario) => {
          const rows = parameters.margins.filter((item) => item.scenario === scenario.value);
          return (
            <section className="admin-parameter-group" key={scenario.value}>
              <header className="admin-panel-heading">
                <div><p className="eyebrow">Scénario de financement</p><h2>{scenario.label}</h2></div>
                <span className="admin-data-chip">{rows.length} produit{rows.length === 1 ? "" : "s"}</span>
              </header>
              {rows.length === 0 ? <div className="card admin-empty-parameter">Aucun produit configuré pour ce scénario.</div> : (
                <div className="admin-product-grid">
                  {rows.map((item) => (
                    <form className="card admin-product-card" key={item.id} onSubmit={(event) => saveMargin(event, item)}>
                      <div className="admin-product-heading"><span className="admin-product-icon"><Settings2 size={18} /></span><div><small>{scenario.label}</small><h3>{item.creditType}</h3></div></div>
                      <div className="admin-product-fields">
                        <label>Marge annuelle (%)
                          <input
                            type="number"
                            required
                            min="0"
                            max="100"
                            step="0.0001"
                            value={margins[item.id]?.marginPercent ?? ""}
                            onChange={(event) => changeMargin(item.id, "marginPercent", event.target.value)}
                          />
                        </label>
                        <label>Durée maximale (mois)
                          <input
                            type="number"
                            required
                            min="1"
                            max="1200"
                            step="1"
                            value={margins[item.id]?.maxMonths ?? ""}
                            onChange={(event) => changeMargin(item.id, "maxMonths", event.target.value)}
                          />
                        </label>
                      </div>
                      <footer className="admin-product-footer">
                        <span>Les changements s’appliquent à la configuration serveur.</span>
                        <button className={`button ${savedRows[item.id] ? "button-quiet" : "button-primary"}`} type="submit" disabled={busy || !dirtyMargins[item.id]}>
                          {savedRows[item.id] ? <Check size={16} /> : <Save size={16} />}
                          {savedRows[item.id] ? "Enregistré" : "Enregistrer"}
                        </button>
                      </footer>
                    </form>
                  ))}
                </div>
              )}
            </section>
          );
        })}
        <p className="admin-parameter-disclaimer"><RotateCcw size={14} /> Les valeurs affichées reflètent les paramètres retournés par le backend. Aucun historique de modification n’est disponible dans l’interface actuelle.</p>
      </> : <div className="card admin-loading-panel">Les paramètres n’ont pas pu être chargés. Consultez le message d’erreur puis réessayez.</div>}
    </section>
  );
}
