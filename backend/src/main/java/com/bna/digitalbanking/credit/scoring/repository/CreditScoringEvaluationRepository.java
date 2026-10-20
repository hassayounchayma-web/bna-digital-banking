package com.bna.digitalbanking.credit.scoring.repository;

import java.util.Optional;
import com.bna.digitalbanking.credit.scoring.entity.CreditScoringEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditScoringEvaluationRepository extends JpaRepository<CreditScoringEvaluation, Long> {
    Optional<CreditScoringEvaluation> findByApplicationId(Long applicationId);
}
