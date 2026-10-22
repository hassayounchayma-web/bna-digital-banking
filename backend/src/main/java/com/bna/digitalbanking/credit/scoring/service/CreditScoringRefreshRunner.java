package com.bna.digitalbanking.credit.scoring.service;

import com.bna.digitalbanking.credit.scoring.entity.CreditScoringEvaluation;
import com.bna.digitalbanking.credit.scoring.repository.CreditScoringEvaluationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreditScoringRefreshRunner implements CommandLineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreditScoringRefreshRunner.class);

    private final CreditScoringEvaluationRepository evaluations;
    private final CreditScoringService scoring;

    public CreditScoringRefreshRunner(
            CreditScoringEvaluationRepository evaluations,
            CreditScoringService scoring) {
        this.evaluations = evaluations;
        this.scoring = scoring;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int refreshedCount = refreshExistingEvaluations();
        if (refreshedCount > 0) {
            LOGGER.info("Refreshed {} persisted credit scoring evaluations using the current criteria.", refreshedCount);
        }
    }

    @Transactional
    public int refreshExistingEvaluations() {
        int refreshedCount = 0;
        for (CreditScoringEvaluation existing : evaluations.findAll()) {
            CreditScoringEvaluation refreshed = scoring.evaluate(
                existing.getApplication(),
                existing.getApplication().getSimulationSnapshot());
            existing.refreshFrom(refreshed);
            refreshedCount++;
        }
        return refreshedCount;
    }
}
