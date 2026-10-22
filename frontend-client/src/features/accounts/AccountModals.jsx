import { ArrowDownUp, X } from "lucide-react";
import { currency } from "./accountData";

function ModalFrame({ title, icon: Icon, onClose, children }) {
  return (
    <div className="modal-backdrop" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose();
    }}>
      <section className="form-modal" role="dialog" aria-modal="true" aria-labelledby="account-modal-title">
        <header className="form-modal-heading">
          <span className="form-modal-icon"><Icon size={20} /></span>
          <div><p className="eyebrow">Opération fictive</p><h2 id="account-modal-title">{title}</h2></div>
          <button type="button" className="modal-close" aria-label="Fermer" onClick={onClose}><X size={19} /></button>
        </header>
        {children}
      </section>
    </div>
  );
}

export default function AccountModals({
  modal,
  onClose,
  accounts,
  operationCategories,
  selectedAccountId,
  onSelectedAccountChange,
  operation,
  onOperationChange,
  onOperationTypeChange,
  onSubmitOperation,
  transfer,
  onTransferChange,
  onSubmitTransfer,
  busy,
}) {
  if (!modal) return null;

  if (modal === "operation") {
    return <ModalFrame title="Ajouter une opération" icon={ArrowDownUp} onClose={onClose}>
      <form onSubmit={onSubmitOperation}>
        <p className="modal-intro">Enregistrez un revenu ou une dépense pour mettre à jour vos données de démonstration.</p>
        <label>Compte
          <select required value={selectedAccountId} onChange={(event) => onSelectedAccountChange(event.target.value)}>
            {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
          </select>
        </label>
        <div className="field-grid">
          <label>Type d’opération
            <select value={operation.type} onChange={(event) => onOperationTypeChange(event.target.value)}>
              <option value="INCOME">Revenu</option><option value="EXPENSE">Dépense</option>
            </select>
          </label>
          <label>Catégorie
            <select required value={operation.category} onChange={(event) => onOperationChange("category", event.target.value)}>
              <option value="">Choisir une catégorie</option>
              {operationCategories.map((category) => <option key={category.code} value={category.code}>{category.label}</option>)}
            </select>
          </label>
        </div>
        <div className="field-grid">
          <label>Montant (DT)
            <input required type="number" min="0.01" step="0.01" value={operation.amount} onChange={(event) => onOperationChange("amount", event.target.value)} />
          </label>
          <label>Date
            <input required type="date" value={operation.occurredAt} onChange={(event) => onOperationChange("occurredAt", event.target.value)} />
          </label>
        </div>
        <label>Description (facultative)
          <input maxLength="200" value={operation.description} onChange={(event) => onOperationChange("description", event.target.value)} />
        </label>
        <div className="modal-actions">
          <button type="button" className="button button-quiet" onClick={onClose}>Annuler</button>
          <button className="button button-primary" disabled={busy || !selectedAccountId}>{busy ? "Enregistrement…" : "Enregistrer"}</button>
        </div>
      </form>
    </ModalFrame>;
  }

  return <ModalFrame title="Effectuer un virement" icon={ArrowDownUp} onClose={onClose}>
    <form onSubmit={onSubmitTransfer}>
      <p className="modal-intro">Virement interne fictif entre vos comptes de démonstration.</p>
      <label>Compte source
        <select required value={transfer.sourceAccountId} onChange={(event) => onTransferChange("sourceAccountId", event.target.value)}>
          {accounts.map((account) => <option key={account.id} value={account.id}>{account.name} · {currency.format(account.currentBalance)}</option>)}
        </select>
      </label>
      <label>Compte destinataire
        <select required value={transfer.destinationAccountId} onChange={(event) => onTransferChange("destinationAccountId", event.target.value)}>
          {accounts.map((account) => <option key={account.id} value={account.id} disabled={String(account.id) === transfer.sourceAccountId}>{account.name}</option>)}
        </select>
      </label>
      <div className="field-grid">
        <label>Montant (DT)
          <input required type="number" min="0.01" step="0.01" value={transfer.amount} onChange={(event) => onTransferChange("amount", event.target.value)} />
        </label>
        <label>Date
          <input required type="date" value={transfer.occurredAt} onChange={(event) => onTransferChange("occurredAt", event.target.value)} />
        </label>
      </div>
      <label>Description (facultative)
        <input maxLength="200" value={transfer.description} onChange={(event) => onTransferChange("description", event.target.value)} />
      </label>
      <p className="field-hint">Le transfert est débité et crédité simultanément ; il ne compte ni comme revenu ni comme dépense du budget.</p>
      <div className="modal-actions">
        <button type="button" className="button button-quiet" onClick={onClose}>Annuler</button>
        <button className="button button-primary" disabled={busy || accounts.length < 2}>{busy ? "Enregistrement…" : "Effectuer le virement"}</button>
      </div>
    </form>
  </ModalFrame>;
}
