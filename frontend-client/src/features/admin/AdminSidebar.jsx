import {
  Gauge,
  Settings2,
  ShieldCheck,
  SlidersHorizontal,
} from "lucide-react";

const items = [
  { id: "dashboard", label: "Vue d’ensemble", icon: Gauge },
  { id: "applications", label: "Demandes de crédit", icon: ShieldCheck },
  { id: "parameters", label: "Paramètres", icon: SlidersHorizontal },
];

export default function AdminSidebar({ section, onNavigate, open, onClose }) {
  const activeSection = section === "detail" ? "applications" : section;
  return (
    <>
      {open && <button className="admin-sidebar-backdrop" type="button" aria-label="Fermer la navigation" onClick={onClose} />}
      <aside className={`admin-sidebar${open ? " is-open" : ""}`} aria-label="Navigation administrateur">
        <a className="admin-brand" href="#admin-dashboard" onClick={(event) => {
          event.preventDefault();
          onNavigate("dashboard");
          onClose();
        }}>
          <span className="admin-brand-mark">B</span>
          <span>BNA <small>Administration</small></span>
        </a>
        <p className="admin-sidebar-caption">Pilotage bancaire</p>
        <nav className="admin-sidebar-nav">
          {items.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              className={`admin-sidebar-link${activeSection === id ? " active" : ""}`}
              type="button"
              aria-current={activeSection === id ? "page" : undefined}
              onClick={() => {
                onNavigate(id);
                onClose();
              }}
            >
              <Icon size={19} strokeWidth={1.8} aria-hidden="true" />
              <span>{label}</span>
            </button>
          ))}
        </nav>
        <div className="admin-sidebar-note">
          <Settings2 size={17} aria-hidden="true" />
          <span>Les décisions de crédit restent humaines.</span>
        </div>
      </aside>
    </>
  );
}
