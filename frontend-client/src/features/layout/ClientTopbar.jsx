import { LogOut, Menu, Search, UserRound } from "lucide-react";
import { NotificationBell } from "../notifications/Notifications";

export default function ClientTopbar({
  user,
  notifications,
  unreadCount,
  onOpenNotification,
  onMarkRead,
  onMarkAllAsRead,
  onViewAllNotifications,
  onLogout,
  onMenuToggle,
}) {
  return (
    <header className="client-topbar">
      <div className="topbar-leading">
        <button className="mobile-menu-button" type="button" aria-label="Ouvrir la navigation" onClick={onMenuToggle}>
          <Menu size={21} />
        </button>
        <div className="topbar-search" aria-label="Espace client BNA">
          <Search size={17} aria-hidden="true" />
          <span>Votre espace bancaire</span>
        </div>
      </div>
      <div className="topbar-actions">
        <NotificationBell
          notifications={notifications}
          unreadCount={unreadCount}
          onOpen={onOpenNotification}
          onMarkRead={onMarkRead}
          onMarkAllAsRead={onMarkAllAsRead}
          onViewAll={onViewAllNotifications}
        />
        <div className="profile-chip">
          <span className="profile-avatar"><UserRound size={17} aria-hidden="true" /></span>
          <span className="profile-name">{user.fullName}</span>
        </div>
        <button className="topbar-logout" type="button" onClick={onLogout} aria-label="Se déconnecter" title="Se déconnecter">
          <LogOut size={18} aria-hidden="true" />
        </button>
      </div>
    </header>
  );
}
