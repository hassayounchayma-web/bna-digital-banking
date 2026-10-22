import {
  Bell,
  CreditCard,
  FileText,
  Gauge,
  LayoutDashboard,
  WalletCards,
} from "lucide-react";

const navigation = [
  { id: "dashboard", label: "Dashboard", icon: LayoutDashboard },
  { id: "accounts", label: "Mes comptes", icon: WalletCards },
  { id: "operations", label: "Opérations", icon: CreditCard },
  { id: "budget", label: "Mon budget", icon: Gauge },
  { id: "simulation", label: "Simulation de crédit", icon: FileText },
  { id: "applications", label: "Mes demandes", icon: FileText },
  { id: "notifications", label: "Notifications", icon: Bell },
];

export default function ClientSidebar({
  activeSection,
  applicationCount,
  unreadCount,
  onNavigate,
  open,
  onClose,
}) {
  return (
    <>
      {open && <button className="sidebar-backdrop" type="button" aria-label="Fermer le menu" onClick={onClose} />}
      <aside className={`client-sidebar${open ? " is-open" : ""}`} aria-label="Navigation principale">
        <a className="sidebar-brand" href="#dashboard" onClick={(event) => {
          event.preventDefault();
          onNavigate("dashboard");
        }}>
          <span className="brand-mark">B</span>
          <span>BNA <small>Digital Banking</small></span>
        </a>
        <p className="sidebar-caption">Espace client</p>
        <nav className="sidebar-navigation">
          {navigation.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              type="button"
              className={`sidebar-link${activeSection === id ? " active" : ""}`}
              aria-current={activeSection === id ? "page" : undefined}
              onClick={() => {
                onNavigate(id);
                onClose();
              }}
            >
              <Icon size={19} strokeWidth={1.8} aria-hidden="true" />
              <span>{label}</span>
              {id === "applications" && applicationCount > 0 && <span className="sidebar-count">{applicationCount}</span>}
              {id === "notifications" && unreadCount > 0 && <span className="sidebar-count">{unreadCount > 99 ? "99+" : unreadCount}</span>}
            </button>
          ))}
        </nav>
        <div className="sidebar-demo-note">
          <span className="demo-note-dot" />
          <div><strong>Espace de démonstration</strong><small>Comptes et opérations fictifs</small></div>
        </div>
      </aside>
    </>
  );
}
