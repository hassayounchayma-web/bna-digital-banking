const labels = {
  DRAFT: "Brouillon",
  SUBMITTED: "Soumise",
  UNDER_REVIEW: "En examen",
  ADDITIONAL_INFO_REQUIRED: "Complément requis",
  APPROVED: "Acceptée",
  REJECTED: "Refusée",
  WITHDRAWN: "Retirée",
};

export function applicationStatusLabel(status) {
  return labels[status] || status?.replaceAll("_", " ") || "Statut inconnu";
}

export function applicationScenarioLabel(scenario) {
  return typeof scenario === "string" && scenario.length > 0
    ? scenario.replaceAll("_", " ")
    : "Scénario non renseigné";
}
