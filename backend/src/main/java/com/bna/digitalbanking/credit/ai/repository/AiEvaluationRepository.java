package com.bna.digitalbanking.credit.ai.repository;

import java.util.Optional;
import com.bna.digitalbanking.credit.ai.entity.AiEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiEvaluationRepository extends JpaRepository<AiEvaluation, Long> {
    Optional<AiEvaluation> findByApplicationId(Long applicationId);
}
