import { useCallback, useEffect, useMemo, useState } from "react";
import api from "../../services/api";

const currency = new Intl.NumberFormat("fr-TN", { style: "currency", currency: "TND" });

function today() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function currentMonth() {
  return today().slice(0, 7);
}

function messageFrom(error) {
  return error.response?.data?.detail
    || error.response?.data?.error
    || error.response?.data?.message
    || "Impossible de contacter le service.";
}

const transactionLabels = {
  INCOME: "Revenu",
  EXPENSE: "Dépense",
  TRANSFER_IN: "Virement reçu",
  TRANSFER_OUT: "Virement envoyé",
};

export default function AccountBudget() {
  const [accounts, setAccounts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [budget, setBudget] = useState(null);
  const [selectedAccountId, setSelectedAccountId] = useState("");
  const [month, setMonth] = useState(currentMonth);
  const [operation, setOperation] = useState({
    type: "EXPENSE",
    category: "",
    amount: "",
    occurredAt: today(),
    description: "",
  });
  const [transfer, setTransfer] = useState({
    sourceAccountId: "",
    destinationAccountId: "",
    amount: "",
    occurredAt: today(),
    description: "",
  });
  const [limits, setLimits] = useState({});
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");
  const [noticeKind, setNoticeKind] = useState("error");

  const operationCategories = useMemo(
    () => categories.filter((category) => category.budgetedExpense === (operation.type === "EXPENSE")),
    [categories, operation.type],
  );

  const loadAccounts = useCallback(async () => {
    const { data } = await api.get("/accounts");
    setAccounts(data);
    setSelectedAccountId((current) => data.some((account) => String(account.id) === String(current))
      ? current
      : String(data[0]?.id ?? ""));
    setTransfer((current) => ({
      ...current,
      sourceAccountId: current.sourceAccountId || String(data[0]?.id ?? ""),
      destinationAccountId: current.destinationAccountId || String(data[1]?.id ?? ""),
    }));
  }, []);

  const loadBudget = useCallback(async () => {
    if (!month) return;
    const { data } = await api.get("/budget", { params: { month } });
    setBudget(data);
    setLimits(Object.fromEntries(data.categories.map((category) => [
      category.category,
      category.monthlyLimit ?? "",
    ])));
  }, [month]);

  const loadTransactions = useCallback(async () => {
    if (!selectedAccountId) {
      return;
    }
    const { data } = await api.get(`/accounts/${selectedAccountId}/transactions`);
    setTransactions(data);
  }, [selectedAccountId]);

  useEffect(() => {
    let active = true;
    async function loadInitialData() {
      try {
        const [accountResponse, categoryResponse] = await Promise.all([
          api.get("/accounts"),
          api.get("/accounts/categories"),
        ]);
        if (!active) return;
        setAccounts(accountResponse.data);
        setSelectedAccountId(String(accountResponse.data[0]?.id ?? ""));
        setTransfer((current) => ({
          ...current,
          sourceAccountId: current.sourceAccountId || String(accountResponse.data[0]?.id ?? ""),
          destinationAccountId: current.destinationAccountId || String(accountResponse.data[1]?.id ?? ""),
        }));
        setCategories(categoryResponse.data);
      } catch (error) {
        if (active) setNotice(messageFrom(error));
      }
    }
    loadInitialData();
    return () => { active = false; };
  }, []);

  useEffect(() => {
    if (!month) return undefined;
    let active = true;
    api.get("/budget", { params: { month } })
      .then(({ data }) => {
        if (!active) return;
        setBudget(data);
        setLimits(Object.fromEntries(data.categories.map((category) => [
          category.category,
          category.monthlyLimit ?? "",
        ])));
      })
      .catch((error) => {
        if (active) setNotice(messageFrom(error));
      });
    return () => { active = false; };
  }, [month]);

  useEffect(() => {
    if (!selectedAccountId) return undefined;
    let active = true;
    api.get(`/accounts/${selectedAccountId}/transactions`)
      .then(({ data }) => {
        if (active) setTransactions(data);
      })
      .catch((error) => {
        if (active) setNotice(messageFrom(error));
      });
    return () => { active = false; };
  }, [selectedAccountId]);

  async function refreshAccountData() {
    await Promise.all([loadAccounts(), loadTransactions(), loadBudget()]);
  }

  async function submitOperation(event) {
    event.preventDefault();
    setBusy(true);
    setNotice("");
    try {
      await api.post(`/accounts/${selectedAccountId}/transactions`, {
        ...operation,
        amount: Number(operation.amount),
      });
      await refreshAccountData();
      setOperation((current) => ({ ...current, amount: "", description: "" }));
      setNotice("L’opération fictive a été enregistrée.");
      setNoticeKind("success");
    } catch (error) {
      setNotice(messageFrom(error));
      setNoticeKind("error");
    } finally {
      setBusy(false);
    }
  }

  async function submitTransfer(event) {
    event.preventDefault();
    setBusy(true);
    setNotice("");
    try {
      await api.post("/accounts/transfers", {
        ...transfer,
        amount: Number(transfer.amount),
      });
      await refreshAccountData();
      setTransfer((current) => ({ ...current, amount: "", description: "" }));
      setNotice("Le virement interne fictif a été enregistré.");
      setNoticeKind("success");
    } catch (error) {
      setNotice(messageFrom(error));
      setNoticeKind("error");
    } finally {
      setBusy(false);
    }
  }

  async function saveBudgetLimit(event, category) {
    event.preventDefault();
    setBusy(true);
    setNotice("");
    try {
      const { data } = await api.put("/budget", {
        month,
        category,
        monthlyLimit: Number(limits[category]),
      });
      setBudget(data);
      setLimits(Object.fromEntries(data.categories.map((item) => [
        item.category,
        item.monthlyLimit ?? "",
      ])));
      setNotice("La limite budgétaire fictive a été enregistrée.");
      setNoticeKind("success");
    } catch (error) {
      setNotice(messageFrom(error));
      setNoticeKind("error");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="account-budget">
      <div className="page-heading">
        <p className="eyebrow">Espace client · données de démonstration</p>
        <h1>Mes comptes et mon budget</h1>
        <p>Comptes, soldes et opérations fictifs : aucun service bancaire réel n’est connecté.</p>
      </div>
      {notice && <p className={`notice ${noticeKind}`} role="status">{notice}</p>}

      <section className="account-cards" aria-label="Comptes fictifs">
        {accounts.map((account) => (
          <button
            type="button"
            className={`card account-card ${String(account.id) === selectedAccountId ? "active" : ""}`}
            key={account.id}
            onClick={() => setSelectedAccountId(String(account.id))}
          >
            <span>{account.name}</span>
            <strong>{currency.format(account.currentBalance)}</strong>
            <small>Solde initial fictif : {currency.format(account.openingBalance)}</small>
            <small>Référence {account.reference.slice(0, 8)} · {account.currency}</small>
          </button>
        ))}
        {accounts.length === 0 && <div className="card empty-card">Chargement des comptes…</div>}
      </section>

      <div className="account-actions-grid">
        <form className="card account-form" onSubmit={submitOperation}>
          <h2>Ajouter un revenu ou une dépense fictive</h2>
          <label>Compte
            <select required value={selectedAccountId} onChange={(event) => setSelectedAccountId(event.target.value)}>
              {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
            </select>
          </label>
          <label>Type d’opération
            <select value={operation.type} onChange={(event) => setOperation((current) => ({
              ...current,
              type: event.target.value,
              category: "",
            }))}>
              <option value="INCOME">Revenu</option>
              <option value="EXPENSE">Dépense</option>
            </select>
          </label>
          <label>Catégorie
            <select required value={operation.category} onChange={(event) => setOperation((current) => ({ ...current, category: event.target.value }))}>
              <option value="">Choisir une catégorie</option>
              {operationCategories.map((category) => <option key={category.code} value={category.code}>{category.label}</option>)}
            </select>
          </label>
          <div className="field-grid">
            <label>Montant (DT)
              <input required type="number" min="0.01" step="0.01" value={operation.amount} onChange={(event) => setOperation((current) => ({ ...current, amount: event.target.value }))} />
            </label>
            <label>Date
              <input required type="date" value={operation.occurredAt} onChange={(event) => setOperation((current) => ({ ...current, occurredAt: event.target.value }))} />
            </label>
          </div>
          <label>Description (facultative)
            <input maxLength="200" value={operation.description} onChange={(event) => setOperation((current) => ({ ...current, description: event.target.value }))} />
          </label>
          <button className="button button-primary" disabled={busy || !selectedAccountId}>{busy ? "Enregistrement…" : "Enregistrer l’opération"}</button>
        </form>

        <form className="card account-form" onSubmit={submitTransfer}>
          <h2>Virement interne fictif</h2>
          <label>Compte source
            <select required value={transfer.sourceAccountId} onChange={(event) => setTransfer((current) => {
              const sourceAccountId = event.target.value;
              const destinationAccountId = sourceAccountId === current.destinationAccountId
                ? String(accounts.find((account) => String(account.id) !== sourceAccountId)?.id ?? "")
                : current.destinationAccountId;
              return { ...current, sourceAccountId, destinationAccountId };
            })}>
              {accounts.map((account) => <option key={account.id} value={account.id}>{account.name} · {currency.format(account.currentBalance)}</option>)}
            </select>
          </label>
          <label>Compte destinataire
            <select required value={transfer.destinationAccountId} onChange={(event) => setTransfer((current) => ({ ...current, destinationAccountId: event.target.value }))}>
              {accounts.map((account) => <option key={account.id} value={account.id} disabled={String(account.id) === transfer.sourceAccountId}>{account.name}</option>)}
            </select>
          </label>
          <div className="field-grid">
            <label>Montant (DT)
              <input required type="number" min="0.01" step="0.01" value={transfer.amount} onChange={(event) => setTransfer((current) => ({ ...current, amount: event.target.value }))} />
            </label>
            <label>Date
              <input required type="date" value={transfer.occurredAt} onChange={(event) => setTransfer((current) => ({ ...current, occurredAt: event.target.value }))} />
            </label>
          </div>
          <label>Description (facultative)
            <input maxLength="200" value={transfer.description} onChange={(event) => setTransfer((current) => ({ ...current, description: event.target.value }))} />
          </label>
          <p className="field-hint">Le virement est débité et crédité simultanément ; il ne compte ni comme revenu ni comme dépense du budget.</p>
          <button className="button button-primary" disabled={busy || accounts.length < 2}>{busy ? "Enregistrement…" : "Effectuer le virement fictif"}</button>
        </form>
      </div>

      <section className="card transaction-section">
        <div className="section-heading">
          <h2>Opérations du compte</h2>
          <label>Compte affiché
            <select value={selectedAccountId} onChange={(event) => setSelectedAccountId(event.target.value)}>
              {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
            </select>
          </label>
        </div>
        {transactions.length === 0 ? <p className="empty-card">Aucune opération enregistrée sur ce compte.</p> : (
          <div className="table-wrap"><table>
            <thead><tr><th>Date</th><th>Opération</th><th>Catégorie</th><th>Description</th><th>Montant</th></tr></thead>
            <tbody>{transactions.map((transaction) => (
              <tr key={transaction.id}>
                <td>{transaction.occurredAt}</td>
                <td>{transactionLabels[transaction.type]}</td>
                <td>{transaction.categoryLabel ?? "Virement interne"}</td>
                <td>{transaction.description || "—"}</td>
                <td>{currency.format(transaction.amount)}</td>
              </tr>
            ))}</tbody>
          </table></div>
        )}
      </section>

      <section className="card budget-section">
        <div className="section-heading">
          <div><p className="eyebrow">Suivi à partir des opérations</p><h2>Self-budgeting mensuel</h2></div>
          <label>Mois
            <input type="month" value={month} onChange={(event) => setMonth(event.target.value)} />
          </label>
        </div>
        <p className="budget-totals">Revenus : {currency.format(budget?.totalIncome ?? 0)} · Dépenses : {currency.format(budget?.totalExpenses ?? 0)}</p>
        <p className="field-hint">Les limites sont des objectifs personnels : un dépassement est signalé, mais n’empêche pas d’enregistrer une dépense.</p>
        {!budget ? <p>Chargement du budget…</p> : (
          <div className="table-wrap"><table>
            <thead><tr><th>Catégorie</th><th>Limite mensuelle (DT)</th><th>Dépensé</th><th>Restant</th><th></th></tr></thead>
            <tbody>{budget.categories.map((category) => (
              <tr key={category.category} className={category.exceeded ? "budget-exceeded" : ""}>
                <td>{category.label}</td>
                <td><input
                  className="table-input"
                  aria-label={`Limite mensuelle ${category.label}`}
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={limits[category.category] ?? ""}
                  onChange={(event) => setLimits((current) => ({ ...current, [category.category]: event.target.value }))}
                /></td>
                <td>{currency.format(category.spent)}</td>
                <td>{category.remaining == null ? "—" : currency.format(category.remaining)}</td>
                <td><button type="button" className="button button-small" disabled={busy || !month || !limits[category.category]} onClick={(event) => saveBudgetLimit(event, category.category)}>Enregistrer</button></td>
              </tr>
            ))}</tbody>
          </table></div>
        )}
      </section>
    </section>
  );
}
