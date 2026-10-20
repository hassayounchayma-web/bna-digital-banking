package com.bna.digitalbanking.credit.ai.repository;

import java.util.Optional;
import com.bna.digitalbanking.credit.ai.entity.EvaluationComparison;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationComparisonRepository extends JpaRepository<EvaluationComparison, Long> {
    Optional<EvaluationComparison> findByApplicationId(Long applicationId);
}
