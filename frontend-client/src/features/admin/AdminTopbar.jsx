import { Menu, UserRound, LogOut } from "lucide-react";

const headings = {
  dashboard: ["Administration", "Vue d’ensemble"],
  applications: ["Crédit", "Demandes de crédit"],
  detail: ["Crédit / Demandes", "Fiche de demande"],
  parameters: ["Configuration", "Paramètres du simulateur"],
};

export default function AdminTopbar({ section, user, onLogout, onMenuToggle }) {
  const [breadcrumb, title] = headings[section] || headings.dashboard;
  return (
    <header className="admin-topbar">
      <div className="admin-topbar-leading">
        <button className="admin-mobile-menu" type="button" aria-label="Ouvrir la navigation" onClick={onMenuToggle}>
          <Menu size={20} />
        </button>
        <div>
          <p className="admin-breadcrumb">{breadcrumb}</p>
          <strong>{title}</strong>
        </div>
      </div>
      <div className="admin-topbar-profile">
        <span className="admin-profile-avatar"><UserRound size={17} aria-hidden="true" /></span>
        <span className="admin-profile-name">{user?.fullName || "Administrateur"}<small>Administration</small></span>
        <button type="button" className="admin-logout" onClick={onLogout} aria-label="Se déconnecter" title="Se déconnecter">
          <LogOut size={18} aria-hidden="true" />
        </button>
      </div>
    </header>
  );
}
