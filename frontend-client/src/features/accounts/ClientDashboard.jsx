import {
  ArrowDownLeft,
  ArrowRight,
  ArrowUpRight,
  Bell,
  CreditCard,
  FileText,
  Plus,
  Wallet,
} from "lucide-react";
import { currency, transactionLabels } from "./accountData";

function DashboardLink({ children, onClick }) {
  return <button className="dashboard-link" type="button" onClick={onClick}>{children}<ArrowRight size={15} /></button>;
}

export default function ClientDashboard({
  user,
  accounts,
  transactions,
  budget,
  applications,
  notifications,
  unreadCount,
  onNavigate,
}) {
  const balance = accounts.reduce((sum, account) => sum + Number(account.currentBalance || 0), 0);
  const latestTransactions = transactions.slice(0, 4);
  const activeApplications = applications.filter((application) => !["APPROVED", "REJECTED", "WITHDRAWN"].includes(application.status));
  const recentNotification = notifications[0];

  return (
    <div className="dashboard-page">
      <section className="dashboard-welcome">
        <div>
          <p className="eyebrow">Vue d’ensemble · données de démonstration</p>
          <h1>Bonjour {String(user?.fullName || "client").split(" ")[0]} <span aria-hidden="true">👋</span></h1>
          <p>Voici le résumé de votre espace BNA aujourd’hui.</p>
        </div>
      </section>

      <section className="dashboard-metrics" aria-label="Synthèse financière">
        <article className="metric-card balance-metric">
          <div className="metric-label"><span className="metric-icon"><Wallet size={19} /></span>Solde total fictif</div>
          <strong>{currency.format(balance)}</strong>
          <small>{accounts.length} compte{accounts.length === 1 ? "" : "s"} de démonstration</small>
        </article>
        <article className="metric-card">
          <div className="metric-label"><span className="metric-icon income-icon"><ArrowDownLeft size={19} /></span>Revenus du mois</div>
          <strong>{currency.format(budget?.totalIncome ?? 0)}</strong>
          <small>Selon vos opérations enregistrées</small>
        </article>
        <article className="metric-card">
          <div className="metric-label"><span className="metric-icon expense-icon"><ArrowUpRight size={19} /></span>Dépenses du mois</div>
          <strong>{currency.format(budget?.totalExpenses ?? 0)}</strong>
          <small>Selon vos opérations enregistrées</small>
        </article>
        <article className="metric-card">
          <div className="metric-label"><span className="metric-icon request-icon"><CreditCard size={19} /></span>Demandes en cours</div>
          <strong>{activeApplications.length}</strong>
          <small>{unreadCount} notification{unreadCount === 1 ? "" : "s"} non lue{unreadCount === 1 ? "" : "s"}</small>
        </article>
      </section>

      <section className="dashboard-columns">
        <article className="card dashboard-panel">
          <header className="dashboard-panel-heading">
            <div><p className="eyebrow">Vos produits</p><h2>Mes comptes</h2></div>
            <DashboardLink onClick={() => onNavigate("accounts")}>Voir les comptes</DashboardLink>
          </header>
          <div className="dashboard-account-list">
            {accounts.length === 0 ? <p className="dashboard-empty">Chargement des comptes…</p> : accounts.map((account, index) => (
              <div className="dashboard-account-row" key={account.id}>
                <span className={`dashboard-account-icon account-icon-${index % 2}`}><Wallet size={18} /></span>
                <div><strong>{account.name}</strong><small>Réf. {account.reference?.slice(0, 8)} · {account.currency}</small></div>
                <b>{currency.format(account.currentBalance)}</b>
              </div>
            ))}
          </div>
          <button className="dashboard-action" type="button" onClick={() => onNavigate("operations")}><Plus size={16} /> Ajouter une opération fictive</button>
        </article>

        <article className="card dashboard-panel">
          <header className="dashboard-panel-heading">
            <div><p className="eyebrow">Suivi de vos dépenses</p><h2>Mon budget</h2></div>
            <DashboardLink onClick={() => onNavigate("budget")}>Détails</DashboardLink>
          </header>
          <div className="budget-summary-line">
            <span>Dépenses enregistrées</span><strong>{currency.format(budget?.totalExpenses ?? 0)}</strong>
          </div>
          {budget?.categories?.length ? (
            <div className="dashboard-category-list">
              {budget.categories.slice(0, 4).map((category) => {
                const ratio = category.monthlyLimit > 0 ? Math.min(100, (category.spent / category.monthlyLimit) * 100) : 0;
                return <div className="dashboard-category" key={category.category}>
                  <div><span>{category.label}</span><small>{currency.format(category.spent)}{category.monthlyLimit ? ` / ${currency.format(category.monthlyLimit)}` : ""}</small></div>
                  <div className="progress-track"><span style={{ width: `${ratio}%` }} /></div>
                </div>;
              })}
            </div>
          ) : <p className="dashboard-empty">Aucune catégorie budgétaire disponible.</p>}
        </article>
      </section>

      <section className="dashboard-columns dashboard-lower">
        <article className="card dashboard-panel">
          <header className="dashboard-panel-heading">
            <div><p className="eyebrow">Activité récente</p><h2>Dernières opérations</h2></div>
            <DashboardLink onClick={() => onNavigate("operations")}>Tout voir</DashboardLink>
          </header>
          {latestTransactions.length ? <div className="dashboard-transaction-list">
            {latestTransactions.map((transaction) => {
              const incoming = ["INCOME", "TRANSFER_IN"].includes(transaction.type);
              return <div className="dashboard-transaction-row" key={transaction.id}>
                <span className={`transaction-type-icon${incoming ? " is-income" : ""}`}>{incoming ? <ArrowDownLeft size={17} /> : <ArrowUpRight size={17} />}</span>
                <div><strong>{transaction.categoryLabel || transactionLabels[transaction.type] || "Opération"}</strong><small>{transaction.occurredAt} · {transaction.description || transactionLabels[transaction.type]}</small></div>
                <b className={incoming ? "amount-in" : ""}>{incoming ? "+" : "−"}{currency.format(transaction.amount)}</b>
              </div>;
            })}
          </div> : <p className="dashboard-empty">Aucune opération enregistrée pour le moment.</p>}
        </article>

        <article className="card dashboard-panel dashboard-shortcuts">
          <header className="dashboard-panel-heading"><div><p className="eyebrow">Accès rapide</p><h2>Mon espace</h2></div></header>
          <button type="button" onClick={() => onNavigate("simulation")}><span className="shortcut-icon"><CreditCard size={18} /></span><span><strong>Simulation de crédit</strong><small>Étudiez votre projet</small></span><ArrowRight size={16} /></button>
          <button type="button" onClick={() => onNavigate("applications")}><span className="shortcut-icon"><FileText size={18} /></span><span><strong>Mes demandes</strong><small>{activeApplications.length} en cours</small></span><ArrowRight size={16} /></button>
          <button type="button" onClick={() => onNavigate("notifications")}><span className="shortcut-icon"><Bell size={18} /></span><span><strong>Notifications</strong><small>{recentNotification?.title || "Aucune nouvelle notification"}</small></span><ArrowRight size={16} /></button>
        </article>
      </section>
      <p className="dashboard-disclaimer">Les comptes et opérations affichés sont fictifs et servent uniquement à la démonstration.</p>
    </div>
  );
}
