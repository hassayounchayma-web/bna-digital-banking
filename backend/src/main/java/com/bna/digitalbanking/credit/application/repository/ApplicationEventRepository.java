package com.bna.digitalbanking.credit.application.repository;

import java.util.List;
import com.bna.digitalbanking.credit.application.entity.ApplicationEvent;
import com.bna.digitalbanking.credit.application.entity.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationEventRepository extends JpaRepository<ApplicationEvent, Long> {
    List<ApplicationEvent> findByApplicationIdOrderByCreatedAtAsc(Long applicationId);

    List<ApplicationEvent> findByApplicationIdAndToStatusOrderByCreatedAtAsc(
        Long applicationId,
        ApplicationStatus status);
}
