const riskLabels = {
  LOW: "Faible",
  MODERATE: "Modéré",
  HIGH: "Élevé",
};

export default function CreditScoringPanel({ scoring }) {
  if (!scoring) return null;

  return (
    <section className="credit-scoring-panel">
      <div className="application-heading">
        <div>
          <p className="eyebrow">Indicateur Java de démonstration</p>
          <h3>{scoring.scoreOutOf100}/100 · risque {riskLabels[scoring.riskLevel] ?? scoring.riskLevel}</h3>
        </div>
        <span className={`result-status ${scoring.provisional ? "warning" : "success"}`}>
          {scoring.provisional ? "Provisoire" : "Données disponibles"}
        </span>
      </div>
      {scoring.provisional && <p>Données indisponibles : {scoring.missingInformation.join(", ")}.</p>}
      <dl className="scoring-factors">
        {scoring.factors.map((factor) => (
          <div key={factor.code}>
            <dt>{factor.label}</dt>
            <dd>{factor.available ? `${factor.points}/${factor.maximumPoints} points` : "Non disponible"}</dd>
            {factor.inputValue && <small>{factor.inputValue}</small>}
            <small>{factor.explanation}</small>
          </div>
        ))}
      </dl>
      <p className="field-hint">{scoring.disclaimer}</p>
    </section>
  );
}
