package com.bna.digitalbanking.notification.service;

import java.time.Instant;
import java.util.List;
import com.bna.digitalbanking.credit.application.entity.ApplicationEvent;
import com.bna.digitalbanking.credit.application.entity.ApplicationStatus;
import com.bna.digitalbanking.notification.dto.MarkAllReadResponse;
import com.bna.digitalbanking.notification.dto.NotificationResponse;
import com.bna.digitalbanking.notification.dto.UnreadCountResponse;
import com.bna.digitalbanking.notification.entity.Notification;
import com.bna.digitalbanking.notification.entity.NotificationType;
import com.bna.digitalbanking.notification.repository.NotificationRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository notifications;

    public NotificationService(NotificationRepository notifications) {
        this.notifications = notifications;
    }

    public void createForApplicationEvent(ApplicationEvent event) {
        if (event.getFromStatus() == event.getToStatus()) {
            return;
        }
        NotificationType type = event.getFromStatus() == null
            ? NotificationType.APPLICATION_CREATED
            : NotificationType.APPLICATION_STATUS_CHANGED;
        NotificationContent content = contentFor(event.getToStatus());
        notifications.save(new Notification(
            type,
            content.title(),
            content.message(),
            event.getApplication().getCustomer(),
            event.getApplication(),
            event));
    }

    public List<NotificationResponse> listOwn(String email) {
        return notifications.findByRecipientEmailOrderByCreatedAtDescIdDesc(email).stream()
            .map(NotificationResponse::from)
            .toList();
    }

    public UnreadCountResponse unreadCount(String email) {
        return new UnreadCountResponse(notifications.countByRecipientEmailAndIsReadFalse(email));
    }

    public NotificationResponse markAsRead(String email, Long id) {
        Notification notification = notifications.findByIdAndRecipientEmail(id, email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification introuvable."));
        notification.markAsRead();
        return NotificationResponse.from(notification);
    }

    public MarkAllReadResponse markAllAsRead(String email) {
        return new MarkAllReadResponse(notifications.markAllUnreadAsRead(email, Instant.now()));
    }

    private NotificationContent contentFor(ApplicationStatus status) {
        return switch (status) {
            case DRAFT -> new NotificationContent(
                "Brouillon créé",
                "Votre brouillon de demande de crédit a été enregistré. Vous pourrez le compléter et le soumettre à tout moment.");
            case SUBMITTED -> new NotificationContent(
                "Demande transmise",
                "Votre demande de crédit a été transmise à la BNA. Vous serez informé(e) de la suite de son traitement.");
            case UNDER_REVIEW -> new NotificationContent(
                "Demande en cours d’examen",
                "Votre demande de crédit est prise en charge par nos équipes et est en cours d’examen.");
            case ADDITIONAL_INFO_REQUIRED -> new NotificationContent(
                "Complément demandé",
                "Un complément d’information est nécessaire pour poursuivre l’examen de votre demande. Consultez votre espace client.");
            case APPROVED -> new NotificationContent(
                "Demande acceptée",
                "La décision concernant votre demande de crédit est favorable. Consultez votre espace client pour le suivi.");
            case REJECTED -> new NotificationContent(
                "Demande refusée",
                "La décision concernant votre demande de crédit est défavorable. Consultez votre espace client pour les informations disponibles.");
            case WITHDRAWN -> new NotificationContent(
                "Demande retirée",
                "Votre demande de crédit a été retirée et ne fera plus l’objet d’un examen.");
        };
    }

    private record NotificationContent(String title, String message) {
    }
}
