export const currency = new Intl.NumberFormat("fr-TN", {
  style: "currency",
  currency: "TND",
  minimumFractionDigits: 3,
  maximumFractionDigits: 3,
});

export const transactionLabels = {
  INCOME: "Revenu",
  EXPENSE: "Dépense",
  TRANSFER_IN: "Virement reçu",
  TRANSFER_OUT: "Virement envoyé",
};

export function today() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

export function currentMonth() {
  return today().slice(0, 7);
}
