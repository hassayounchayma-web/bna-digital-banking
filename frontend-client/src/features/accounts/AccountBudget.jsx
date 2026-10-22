import { useCallback, useEffect, useMemo, useState } from "react";
import { ArrowDownUp, ArrowRight, Plus, Wallet } from "lucide-react";
import api from "../../services/api";
import AccountModals from "./AccountModals";
import ClientDashboard from "./ClientDashboard";
import { currency, currentMonth, today, transactionLabels } from "./accountData";

function messageFrom(error) {
  return error.response?.data?.detail
    || error.response?.data?.error
    || error.response?.data?.message
    || "Impossible de contacter le service.";
}

function DashboardLink({ children, onClick }) {
  return <button className="dashboard-link" type="button" onClick={onClick}>{children}<ArrowRight size={15} /></button>;
}

function AccountCards({ accounts, selectedAccountId, onSelect }) {
  return (
    <section className="account-cards" aria-label="Comptes fictifs">
      {accounts.map((account, index) => (
        <button
          type="button"
          className={`card account-card account-card-${index % 2}${String(account.id) === selectedAccountId ? " active" : ""}`}
          key={account.id}
          onClick={() => onSelect(String(account.id))}
        >
          <span className="account-card-top"><span>{account.name}</span><Wallet size={18} /></span>
          <strong>{currency.format(account.currentBalance)}</strong>
          <small>Solde initial fictif : {currency.format(account.openingBalance)}</small>
          <small>Référence {account.reference.slice(0, 8)} · {account.currency}</small>
        </button>
      ))}
      {accounts.length === 0 && <div className="card empty-card">Chargement des comptes…</div>}
    </section>
  );
}

function TransactionTable({ transactions, emptyMessage }) {
  if (!transactions.length) return <div className="table-empty">{emptyMessage}</div>;
  return (
    <div className="table-wrap">
      <table className="modern-table">
        <thead><tr><th>Date</th><th>Opération</th><th>Catégorie</th><th>Description</th><th>Montant</th></tr></thead>
        <tbody>{transactions.map((transaction) => {
          const incoming = ["INCOME", "TRANSFER_IN"].includes(transaction.type);
          return <tr key={transaction.id}>
            <td>{transaction.occurredAt}</td>
            <td><span className={`transaction-badge${incoming ? " is-income" : ""}`}>{transactionLabels[transaction.type]}</span></td>
            <td>{transaction.categoryLabel ?? "Virement interne"}</td>
            <td>{transaction.description || "—"}</td>
            <td className={incoming ? "amount-in" : "amount-out"}>{incoming ? "+" : "−"}{currency.format(transaction.amount)}</td>
          </tr>;
        })}</tbody>
      </table>
    </div>
  );
}

function BudgetCategoryCard({ category, value, onChange, onSave, busy }) {
  const progress = category.monthlyLimit > 0 ? Math.min(100, (category.spent / category.monthlyLimit) * 100) : 0;
  return (
    <article className={`budget-category-card${category.exceeded ? " is-exceeded" : ""}`}>
      <div className="budget-category-heading">
        <div><h3>{category.label}</h3><small>{category.exceeded ? "Limite dépassée" : "Suivi du mois"}</small></div>
        {category.exceeded && <span className="budget-status">Dépassement</span>}
      </div>
      <strong className="budget-spent">{currency.format(category.spent)}</strong>
      {category.monthlyLimit > 0 && <div className="progress-track"><span style={{ width: `${progress}%` }} /></div>}
      <form className="budget-limit-form" onSubmit={(event) => onSave(event, category.category)}>
        <label>Limite mensuelle (DT)
          <input
            type="number"
            min="0.01"
            step="0.01"
            value={value ?? ""}
            onChange={(event) => onChange(category.category, event.target.value)}
            aria-label={`Limite mensuelle ${category.label}`}
          />
        </label>
        <button type="submit" className="button button-small" disabled={busy || !value}>Enregistrer</button>
      </form>
      <p className="budget-remaining">Restant : <strong>{category.remaining == null ? "—" : currency.format(category.remaining)}</strong></p>
    </article>
  );
}

export default function AccountBudget({
  view,
  user,
  applications,
  notifications,
  unreadCount,
  onNavigate,
}) {
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
  const [modal, setModal] = useState(null);
  const [search, setSearch] = useState("");
  const [transactionType, setTransactionType] = useState("ALL");
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
      sourceAccountId: data.some((account) => String(account.id) === current.sourceAccountId)
        ? current.sourceAccountId
        : String(data[0]?.id ?? ""),
      destinationAccountId: data.some((account) => String(account.id) === current.destinationAccountId)
        ? current.destinationAccountId
        : String(data[1]?.id ?? ""),
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
      setTransactions([]);
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
          sourceAccountId: String(accountResponse.data[0]?.id ?? ""),
          destinationAccountId: String(accountResponse.data[1]?.id ?? ""),
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
      setModal(null);
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
      setModal(null);
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

  const visibleTransactions = transactions.filter((transaction) => {
    const matchesType = transactionType === "ALL" || transaction.type === transactionType;
    const query = search.trim().toLocaleLowerCase("fr");
    const matchesSearch = !query || [
      transaction.description,
      transaction.categoryLabel,
      transactionLabels[transaction.type],
    ].some((value) => String(value || "").toLocaleLowerCase("fr").includes(query));
    return matchesType && matchesSearch;
  });

  function renderNotice() {
    return notice ? <p className={`notice ${noticeKind}`} role="status">{notice}</p> : null;
  }

  if (view === "dashboard") {
    return <div className="account-budget">
      {renderNotice()}
      <ClientDashboard
        user={user}
        accounts={accounts}
        transactions={transactions}
        budget={budget}
        applications={applications}
        notifications={notifications}
        unreadCount={unreadCount}
        onNavigate={onNavigate}
      />
    </div>;
  }

  if (view === "accounts") {
    return <section className="account-budget">
      {renderNotice()}
      <div className="page-intro">
        <div><p className="eyebrow">Patrimoine · comptes fictifs</p><h1>Mes comptes</h1><p>Consultez vos soldes et les détails de vos comptes de démonstration.</p></div>
        <button className="button button-primary" type="button" onClick={() => onNavigate("operations")}><Plus size={17} /> Nouvelle opération</button>
      </div>
      <AccountCards accounts={accounts} selectedAccountId={selectedAccountId} onSelect={setSelectedAccountId} />
      <section className="card transaction-section">
        <header className="dashboard-panel-heading">
          <div><p className="eyebrow">Activité récente</p><h2>Opérations du compte</h2></div>
          <label className="compact-field">Compte affiché
            <select value={selectedAccountId} onChange={(event) => setSelectedAccountId(event.target.value)}>
              {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
            </select>
          </label>
        </header>
        <TransactionTable transactions={transactions.slice(0, 5)} emptyMessage="Aucune opération enregistrée sur ce compte." />
        <DashboardLink onClick={() => onNavigate("operations")}>Consulter toutes les opérations</DashboardLink>
      </section>
      <p className="dashboard-disclaimer">Comptes, soldes et opérations fictifs : aucun service bancaire réel n’est connecté.</p>
    </section>;
  }

  if (view === "operations") {
    return <section className="account-budget">
      {renderNotice()}
      <div className="page-intro">
        <div><p className="eyebrow">Activité du compte · données fictives</p><h1>Opérations</h1><p>Retrouvez l’historique et gérez vos mouvements de démonstration.</p></div>
        <div className="page-actions">
          <button className="button button-quiet" type="button" onClick={() => setModal("transfer")} disabled={accounts.length < 2}><ArrowDownUp size={17} /> Virement fictif</button>
          <button className="button button-primary" type="button" onClick={() => setModal("operation")}><Plus size={17} /> Ajouter une opération</button>
        </div>
      </div>
      <div className="operation-summary">
        <div><span>Compte</span><select value={selectedAccountId} onChange={(event) => setSelectedAccountId(event.target.value)}>{accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}</select></div>
        <div><span>Solde actuel fictif</span><strong>{currency.format(accounts.find((account) => String(account.id) === selectedAccountId)?.currentBalance ?? 0)}</strong></div>
        <div><span>Opérations</span><strong>{transactions.length}</strong></div>
      </div>
      <section className="card transaction-section">
        <header className="transaction-toolbar">
          <div><h2>Historique des opérations</h2><p>{visibleTransactions.length} résultat{visibleTransactions.length === 1 ? "" : "s"}</p></div>
          <div className="transaction-filters">
            <input aria-label="Rechercher une opération" placeholder="Rechercher…" value={search} onChange={(event) => setSearch(event.target.value)} />
            <select aria-label="Filtrer par type" value={transactionType} onChange={(event) => setTransactionType(event.target.value)}>
              <option value="ALL">Tous les types</option>
              <option value="INCOME">Revenus</option>
              <option value="EXPENSE">Dépenses</option>
              <option value="TRANSFER_IN">Virements reçus</option>
              <option value="TRANSFER_OUT">Virements envoyés</option>
            </select>
          </div>
        </header>
        <TransactionTable transactions={visibleTransactions} emptyMessage="Aucune opération ne correspond à vos filtres." />
      </section>
      <p className="dashboard-disclaimer">Les mouvements et virements sont fictifs et n’affectent aucun compte bancaire réel.</p>
      <AccountModals
        modal={modal}
        onClose={() => setModal(null)}
        accounts={accounts}
        operationCategories={operationCategories}
        selectedAccountId={selectedAccountId}
        onSelectedAccountChange={setSelectedAccountId}
        operation={operation}
        onOperationChange={(key, value) => setOperation((current) => ({ ...current, [key]: value }))}
        onOperationTypeChange={(type) => setOperation((current) => ({ ...current, type, category: "" }))}
        onSubmitOperation={submitOperation}
        transfer={transfer}
        onTransferChange={(key, value) => setTransfer((current) => {
          if (key === "sourceAccountId" && value === current.destinationAccountId) {
            return { ...current, sourceAccountId: value, destinationAccountId: String(accounts.find((account) => String(account.id) !== value)?.id ?? "") };
          }
          return { ...current, [key]: value };
        })}
        onSubmitTransfer={submitTransfer}
        busy={busy}
      />
    </section>;
  }

  if (view === "budget") {
    return <section className="account-budget">
      {renderNotice()}
      <div className="page-intro">
        <div><p className="eyebrow">Pilotage personnel · données fictives</p><h1>Mon budget</h1><p>Suivez les revenus et dépenses transmis par vos opérations enregistrées.</p></div>
        <label className="compact-field">Période
          <input type="month" value={month} onChange={(event) => setMonth(event.target.value)} />
        </label>
      </div>
      <div className="budget-metrics">
        <article className="metric-card"><span>Revenus du mois</span><strong>{currency.format(budget?.totalIncome ?? 0)}</strong><small>Calculé par le service budget</small></article>
        <article className="metric-card"><span>Dépenses du mois</span><strong>{currency.format(budget?.totalExpenses ?? 0)}</strong><small>Calculé par le service budget</small></article>
        <article className="metric-card"><span>Catégories suivies</span><strong>{budget?.categories?.length ?? 0}</strong><small>Limites et dépenses du mois</small></article>
      </div>
      <section className="budget-category-section">
        <header className="dashboard-panel-heading"><div><p className="eyebrow">Par catégorie</p><h2>Limites et consommation</h2></div></header>
        <p className="field-hint">Les limites sont des objectifs personnels : un dépassement est signalé, mais n’empêche pas d’enregistrer une dépense.</p>
        {!budget ? <div className="card loading-card">Chargement du budget…</div> : budget.categories.length === 0
          ? <div className="card table-empty">Aucune catégorie disponible pour cette période.</div>
          : <div className="budget-category-grid">{budget.categories.map((category) => <BudgetCategoryCard
            key={category.category}
            category={category}
            value={limits[category.category]}
            onChange={(key, value) => setLimits((current) => ({ ...current, [key]: value }))}
            onSave={saveBudgetLimit}
            busy={busy}
          />)}</div>}
      </section>
      <p className="dashboard-disclaimer">Les indicateurs sont calculés à partir des opérations fictives enregistrées.</p>
    </section>;
  }

  return null;
}
