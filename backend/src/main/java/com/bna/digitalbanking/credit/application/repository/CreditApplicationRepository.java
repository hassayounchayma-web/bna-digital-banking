package com.bna.digitalbanking.credit.application.repository;

import java.util.List;
import java.util.Optional;
import com.bna.digitalbanking.credit.application.entity.ApplicationStatus;
import com.bna.digitalbanking.credit.application.entity.CreditApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditApplicationRepository extends JpaRepository<CreditApplication, Long> {
    List<CreditApplication> findByCustomerEmailOrderByUpdatedAtDesc(String email);

    List<CreditApplication> findAllByOrderByUpdatedAtDesc();

    List<CreditApplication> findByStatusOrderByUpdatedAtDesc(ApplicationStatus status);

    List<CreditApplication> findByStatusNotOrderByUpdatedAtDesc(ApplicationStatus status);

    Optional<CreditApplication> findByIdAndCustomerEmail(Long id, String email);
}
