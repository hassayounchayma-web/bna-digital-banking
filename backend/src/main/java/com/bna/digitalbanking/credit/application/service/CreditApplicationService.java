package com.bna.digitalbanking.credit.application.service;

import java.math.BigDecimal;
import java.util.List;
import com.bna.digitalbanking.credit.application.dto.ApplicationDecision;
import com.bna.digitalbanking.credit.application.dto.ApplicationDecisionRequest;
import com.bna.digitalbanking.credit.application.dto.ApplicationEventResponse;
import com.bna.digitalbanking.credit.application.dto.ApplicationNoteRequest;
import com.bna.digitalbanking.credit.application.dto.ApplicationRequest;
import com.bna.digitalbanking.credit.application.dto.ApplicationResponse;
import com.bna.digitalbanking.credit.application.entity.ApplicationEvent;
import com.bna.digitalbanking.credit.application.entity.ApplicationStatus;
import com.bna.digitalbanking.credit.application.entity.CreditApplication;
import com.bna.digitalbanking.credit.application.repository.ApplicationEventRepository;
import com.bna.digitalbanking.credit.application.repository.CreditApplicationRepository;
import com.bna.digitalbanking.credit.ai.dto.AiEvaluationResponse;
import com.bna.digitalbanking.credit.ai.dto.EvaluationComparisonResponse;
import com.bna.digitalbanking.credit.ai.repository.AiEvaluationRepository;
import com.bna.digitalbanking.credit.ai.repository.EvaluationComparisonRepository;
import com.bna.digitalbanking.credit.ai.service.CreditAiEvaluationPersistenceService.AiComparisonResult;
import com.bna.digitalbanking.credit.ai.service.CreditAiEvaluationService;
import com.bna.digitalbanking.credit.scoring.dto.CreditScoringResponse;
import com.bna.digitalbanking.credit.scoring.repository.CreditScoringEvaluationRepository;
import com.bna.digitalbanking.credit.scoring.service.CreditScoringEvidenceService;
import com.bna.digitalbanking.credit.scoring.service.CreditScoringService;
import com.bna.digitalbanking.identity.UserEntity;
import com.bna.digitalbanking.identity.UserRepository;
import com.bna.digitalbanking.identity.UserRole;
import com.bna.digitalbanking.notification.service.NotificationService;
import com.bna.digitalbanking.simulation.CreditParameterService;
import com.bna.digitalbanking.simulation.CreditRateConfig;
import com.bna.digitalbanking.simulation.RachatExterneRequest;
import com.bna.digitalbanking.simulation.RachatInterneRequest;
import com.bna.digitalbanking.simulation.SansRachatRequest;
import com.bna.digitalbanking.simulation.SimulationResult;
import com.bna.digitalbanking.simulation.SimulationService;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CreditApplicationService {
    private final CreditApplicationRepository applications;
    private final ApplicationEventRepository events;
    private final UserRepository users;
    private final CreditParameterService parameters;
    private final SimulationService simulations;
    private final CreditScoringEvaluationRepository scoringEvaluations;
    private final CreditScoringEvidenceService scoringEvidence;
    private final CreditScoringService scoring;
    private final AiEvaluationRepository aiEvaluations;
    private final EvaluationComparisonRepository comparisons;
    private final CreditAiEvaluationService aiEvaluationService;
    private final NotificationService notifications;

    public CreditApplicationService(
            CreditApplicationRepository applications,
            ApplicationEventRepository events,
            UserRepository users,
            CreditParameterService parameters,
            SimulationService simulations,
            CreditScoringEvaluationRepository scoringEvaluations,
            CreditScoringEvidenceService scoringEvidence,
            CreditScoringService scoring,
            AiEvaluationRepository aiEvaluations,
            EvaluationComparisonRepository comparisons,
            CreditAiEvaluationService aiEvaluationService,
            NotificationService notifications) {
        this.applications = applications;
        this.events = events;
        this.users = users;
        this.parameters = parameters;
        this.simulations = simulations;
        this.scoringEvaluations = scoringEvaluations;
        this.scoringEvidence = scoringEvidence;
        this.scoring = scoring;
        this.aiEvaluations = aiEvaluations;
        this.comparisons = comparisons;
        this.aiEvaluationService = aiEvaluationService;
        this.notifications = notifications;
    }

    private SimulationResult simulate(ApplicationRequest request) {
        return switch (request.scenario()) {
            case SANS_RACHAT -> simulations.simulateSansRachat(new SansRachatRequest(
                request.averageMonthlySalary(),
                request.halfRentIncome().multiply(BigDecimal.valueOf(2)),
                request.sedDeductions(),
                request.bnaDeductions(),
                request.creditType(),
                request.durationMonths(),
                request.requestedAmount()));
            case RACHAT_INTERNE -> simulations.simulateRachatInterne(new RachatInterneRequest(
                request.averageMonthlySalary(),
                request.halfRentIncome().multiply(BigDecimal.valueOf(2)),
                request.sedDeductions(),
                request.bnaNonBuyoutDeductions(),
                request.remainingBuyoutCapital(),
                request.creditType(),
                request.durationMonths(),
                request.requestedAmount()));
            case RACHAT_EXTERNE -> simulations.simulateRachatExterne(new RachatExterneRequest(
                request.averageMonthlySalary(),
                request.halfRentIncome().multiply(BigDecimal.valueOf(2)),
                request.sedDeductions(),
                request.remainingBuyoutCapital(),
                request.creditType(),
                request.durationMonths(),
                request.requestedAmount()));
        };
    }

    private ApplicationRequest toRequest(CreditApplication application) {
        return new ApplicationRequest(
            application.getScenario(),
            application.getCreditType(),
            application.getRequestedAmount(),
            application.getDurationMonths(),
            application.getAverageMonthlySalary(),
            application.getHalfRentIncome(),
            application.getSedDeductions(),
            application.getBnaDeductions(),
            application.getBnaNonBuyoutDeductions(),
            application.getRemainingBuyoutCapital());
    }

    public ApplicationResponse createDraft(String email, ApplicationRequest request) {
        validateProduct(request);
        UserEntity customer = findUser(email);
        SimulationResult simulation = simulate(request);
        CreditApplication application = applications.save(new CreditApplication(request, customer, simulation));
        record(application, email, UserRole.CLIENT, null, ApplicationStatus.DRAFT, "Brouillon créé");
        return response(application);
    }

    public List<ApplicationResponse> listOwn(String email) {
        return applications.findByCustomerEmailOrderByUpdatedAtDesc(email).stream()
            .map(this::response)
            .toList();
    }

    public ApplicationResponse getOwn(String email, Long id) {
        return response(findOwned(email, id));
    }

    public ApplicationResponse updateDraft(String email, Long id, ApplicationRequest request) {
        CreditApplication application = findOwned(email, id);
        requireStatus(application, ApplicationStatus.DRAFT);
        validateProduct(request);
        application.updateDetails(request);
        application.updateSimulationSnapshot(simulate(request));
        record(application, email, UserRole.CLIENT, ApplicationStatus.DRAFT, ApplicationStatus.DRAFT, "Brouillon modifié");
        return response(application);
    }

    public ApplicationResponse submit(String email, Long id) {
        CreditApplication application = findOwned(email, id);
        ApplicationStatus previous = application.getStatus();
        if (previous != ApplicationStatus.DRAFT) {
            invalidTransition();
        }
        if (application.getRequestedAmount() == null || application.getRequestedAmount().signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un montant de crédit positif est requis pour soumettre la demande.");
        }
        SimulationResult simulation = simulate(toRequest(application));
        application.updateSimulationSnapshot(simulation);
        if (!"ELIGIBLE".equals(simulation.statut())) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                simulation.error() == null ? "La simulation ne permet pas de soumettre cette demande." : simulation.error());
        }
        CreditScoringEvidenceService.Evidence evidence = scoringEvidence.detect(application.getCustomer());
        application.updateScoringEvidence(
            evidence.incomeStabilityMonths(),
            evidence.savingsRegularityMonthsLast12Months());
        scoringEvaluations.save(scoring.evaluate(application, application.getSimulationSnapshot()));
        application.transition(ApplicationStatus.SUBMITTED);
        record(application, email, UserRole.CLIENT, previous, ApplicationStatus.SUBMITTED, "Demande soumise");
        return response(application);
    }

    public ApplicationResponse provideInfo(String email, Long id, ApplicationNoteRequest request) {
        CreditApplication application = findOwned(email, id);
        requireStatus(application, ApplicationStatus.ADDITIONAL_INFO_REQUIRED);
        application.transition(ApplicationStatus.SUBMITTED);
        record(application, email, UserRole.CLIENT, ApplicationStatus.ADDITIONAL_INFO_REQUIRED,
            ApplicationStatus.SUBMITTED, request.note().strip());
        return response(application);
    }

    public ApplicationResponse withdraw(String email, Long id) {
        CreditApplication application = findOwned(email, id);
        if (application.getStatus() != ApplicationStatus.DRAFT
                && application.getStatus() != ApplicationStatus.SUBMITTED
                && application.getStatus() != ApplicationStatus.UNDER_REVIEW
                && application.getStatus() != ApplicationStatus.ADDITIONAL_INFO_REQUIRED) {
            invalidTransition();
        }
        transition(application, email, UserRole.CLIENT, ApplicationStatus.WITHDRAWN, "Demande retirée");
        return response(application);
    }

    public List<ApplicationResponse> listAll(ApplicationStatus status) {
        List<CreditApplication> result = status == ApplicationStatus.DRAFT
            ? List.of()
            : status == null
                ? applications.findByStatusNotOrderByUpdatedAtDesc(ApplicationStatus.DRAFT)
                : applications.findByStatusOrderByUpdatedAtDesc(status);
        return result.stream().map(application -> response(application, false, true)).toList();
    }

    public ApplicationResponse getAdmin(Long id) {
        return response(find(id), true, true);
    }

    public ApplicationResponse claim(String email, Long id) {
        CreditApplication application = find(id);
        requireStatus(application, ApplicationStatus.SUBMITTED);
        transition(application, email, UserRole.ADMIN, ApplicationStatus.UNDER_REVIEW, "Demande prise en charge");
        return response(application, true, true);
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public AiComparisonResult evaluateAi(String email, Long id) {
        CreditApplication application = find(id);
        requireStatus(application, ApplicationStatus.UNDER_REVIEW);
        return aiEvaluationService.evaluate(id, email);
    }

    public ApplicationResponse requestInfo(String email, Long id, ApplicationNoteRequest request) {
        CreditApplication application = find(id);
        requireStatus(application, ApplicationStatus.UNDER_REVIEW);
        transition(application, email, UserRole.ADMIN, ApplicationStatus.ADDITIONAL_INFO_REQUIRED, request.note().strip());
        return response(application, true, true);
    }

    public ApplicationResponse decide(String email, Long id, ApplicationDecisionRequest request) {
        CreditApplication application = find(id);
        requireStatus(application, ApplicationStatus.UNDER_REVIEW);
        ApplicationStatus decision = request.decision() == ApplicationDecision.APPROVED
            ? ApplicationStatus.APPROVED
            : ApplicationStatus.REJECTED;
        transition(application, email, UserRole.ADMIN, decision, request.reason().strip());
        return response(application, true, true);
    }

    private UserEntity findUser(String email) {
        return users.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    private CreditApplication find(Long id) {
        return applications.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
    }

    private CreditApplication findOwned(String email, Long id) {
        return applications.findByIdAndCustomerEmail(id, email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable"));
    }

    private void transition(
            CreditApplication application,
            String email,
            UserRole role,
            ApplicationStatus status,
            String note) {
        ApplicationStatus previous = application.getStatus();
        application.transition(status);
        record(application, email, role, previous, status, note);
    }

    private void record(
            CreditApplication application,
            String email,
            UserRole role,
            ApplicationStatus previous,
            ApplicationStatus next,
            String note) {
        ApplicationEvent event = events.save(new ApplicationEvent(application, email, role, previous, next, note));
        if (previous != next) {
            notifications.createForApplicationEvent(event);
        }
    }

    private ApplicationResponse response(CreditApplication application) {
        return response(application, true, false);
    }

    private ApplicationResponse response(
            CreditApplication application,
            boolean includeHistory,
            boolean includeAiEvaluation) {
        List<ApplicationEvent> history = includeHistory
            ? events.findByApplicationIdOrderByCreatedAtAsc(application.getId())
            : List.of();
        return new ApplicationResponse(
            application.getId(), application.getReference(),
            application.getCustomer().getFullName(), application.getCustomer().getEmail(),
            application.getScenario(), application.getCreditType(), application.getRequestedAmount(),
            application.getDurationMonths(), application.getAverageMonthlySalary(),
            application.getHalfRentIncome(), application.getSedDeductions(), application.getBnaDeductions(),
            application.getBnaNonBuyoutDeductions(), application.getRemainingBuyoutCapital(),
            application.getStatus(), application.getCreatedAt(), application.getUpdatedAt(),
            application.getSubmittedAt(), application.getIncomeStabilityMonths(),
            application.getSavingsRegularityMonthsLast12Months(),
            history.stream().map(ApplicationEventResponse::from).toList(),
            com.bna.digitalbanking.credit.application.dto.SimulationSnapshotResponse
                .from(application.getSimulationSnapshot()),
            scoringEvaluations.findByApplicationId(application.getId())
                .map(CreditScoringResponse::from)
                .orElse(null),
            includeAiEvaluation
                ? aiEvaluations.findByApplicationId(application.getId())
                    .map(AiEvaluationResponse::from)
                    .orElse(null)
                : null,
            includeAiEvaluation
                ? comparisons.findByApplicationId(application.getId())
                    .map(EvaluationComparisonResponse::from)
                    .orElse(null)
                : null);
    }

    private void requireStatus(CreditApplication application, ApplicationStatus expected) {
        if (application.getStatus() != expected) {
            invalidTransition();
        }
    }

    private void validateProduct(ApplicationRequest request) {
        CreditRateConfig config = parameters.getRateConfig(request.scenario(), request.creditType().strip());
        if (request.durationMonths() > config.maxMonths()) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "La durée demandée dépasse le maximum autorisé pour ce produit.");
        }
    }

    private void invalidTransition() {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette action n’est pas autorisée pour le statut actuel.");
    }
}
