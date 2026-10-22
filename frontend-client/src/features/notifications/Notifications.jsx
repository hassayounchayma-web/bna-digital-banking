import { useEffect, useRef, useState } from "react";
import { Bell } from "lucide-react";

function formatDate(value) {
  return new Date(value).toLocaleString("fr-TN", {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

function notificationTypeLabel(type) {
  return type === "APPLICATION_CREATED" ? "Création de demande" : "Évolution de demande";
}

function NotificationItem({ notification, onOpen, onMarkRead }) {
  return (
    <article className={`notification-item${notification.isRead ? " is-read" : " is-unread"}`}>
      <button className="notification-open" type="button" onClick={() => onOpen(notification)}>
        <span className="notification-item-heading">
          <strong>{notification.title}</strong>
          {!notification.isRead && <span className="notification-unread-dot" aria-label="Non lue" />}
        </span>
        <span>{notification.message}</span>
        <small>{notificationTypeLabel(notification.type)} · {formatDate(notification.createdAt)}</small>
      </button>
      {!notification.isRead && (
        <button
          className="notification-mark-read"
          type="button"
          aria-label={`Marquer « ${notification.title} » comme lue`}
          onClick={() => onMarkRead(notification.id)}
        >
          Marquer comme lue
        </button>
      )}
    </article>
  );
}

export function NotificationBell({
  notifications,
  unreadCount,
  onOpen,
  onMarkRead,
  onMarkAllAsRead,
  onViewAll,
}) {
  const [open, setOpen] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    if (!open) return undefined;
    const closeOnOutsideClick = (event) => {
      if (!menuRef.current?.contains(event.target)) setOpen(false);
    };
    const closeOnEscape = (event) => {
      if (event.key === "Escape") setOpen(false);
    };
    document.addEventListener("pointerdown", closeOnOutsideClick);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("pointerdown", closeOnOutsideClick);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, [open]);

  const handleOpen = async (notification) => {
    setOpen(false);
    await onOpen(notification);
  };

  return (
    <div className="notification-bell-wrap" ref={menuRef}>
      <button
        className="notification-bell"
        type="button"
        aria-label={`Notifications, ${unreadCount} non lue(s)`}
        aria-expanded={open}
        aria-haspopup="true"
        onClick={() => setOpen((current) => !current)}
      >
        <Bell size={20} strokeWidth={1.8} aria-hidden="true" />
        {unreadCount > 0 && <span className="notification-badge">{unreadCount > 99 ? "99+" : unreadCount}</span>}
      </button>
      {open && (
        <section className="notification-dropdown" aria-label="Notifications récentes">
          <header className="notification-dropdown-heading">
            <strong>Notifications</strong>
            {unreadCount > 0 && (
              <button type="button" onClick={onMarkAllAsRead}>Tout lire</button>
            )}
          </header>
          {notifications.length === 0 ? (
            <p className="notification-empty">Aucune notification pour le moment.</p>
          ) : notifications.slice(0, 5).map((notification) => (
            <NotificationItem
              key={notification.id}
              notification={notification}
              onOpen={handleOpen}
              onMarkRead={onMarkRead}
            />
          ))}
          <button
            className="notification-view-all"
            type="button"
            onClick={() => {
              setOpen(false);
              onViewAll();
            }}
          >
            Voir toutes les notifications
          </button>
        </section>
      )}
    </div>
  );
}

export default function NotificationsPage({
  notifications,
  unreadCount,
  onOpen,
  onMarkRead,
  onMarkAllAsRead,
}) {
  return (
    <section className="notifications-page">
      <div className="notifications-page-heading">
        <div>
          <p className="eyebrow">Centre de notifications</p>
          <h2>Historique des notifications</h2>
          <p>Suivez les étapes de traitement de vos demandes de crédit.</p>
        </div>
        {unreadCount > 0 && (
          <button className="button button-quiet" type="button" onClick={onMarkAllAsRead}>
            Tout marquer comme lu
          </button>
        )}
      </div>
      {notifications.length === 0 ? (
        <div className="card notification-empty-page">Vous n’avez pas encore de notification.</div>
      ) : (
        <div className="notification-list">
          {notifications.map((notification) => (
            <NotificationItem
              key={notification.id}
              notification={notification}
              onOpen={onOpen}
              onMarkRead={onMarkRead}
            />
          ))}
        </div>
      )}
    </section>
  );
}
