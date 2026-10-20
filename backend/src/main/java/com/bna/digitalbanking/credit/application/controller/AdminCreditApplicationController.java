package com.bna.digitalbanking.credit.application.controller;

import java.util.List;
import com.bna.digitalbanking.credit.application.dto.ApplicationDecisionRequest;
import com.bna.digitalbanking.credit.application.dto.ApplicationNoteRequest;
import com.bna.digitalbanking.credit.application.dto.ApplicationResponse;
import com.bna.digitalbanking.credit.application.entity.ApplicationStatus;
import com.bna.digitalbanking.credit.application.service.CreditApplicationService;
import com.bna.digitalbanking.credit.ai.service.CreditAiEvaluationPersistenceService.AiComparisonResult;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/applications")
public class AdminCreditApplicationController {
    private final CreditApplicationService service;

    public AdminCreditApplicationController(CreditApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ApplicationResponse> list(@RequestParam(required = false) ApplicationStatus status) {
        return service.listAll(status);
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@PathVariable Long id) {
        return service.getAdmin(id);
    }

    @PostMapping("/{id}/claim")
    public ApplicationResponse claim(@PathVariable Long id, Authentication authentication) {
        return service.claim(authentication.getName(), id);
    }

    @PostMapping("/{id}/ai-evaluation")
    public AiComparisonResult evaluateAi(@PathVariable Long id, Authentication authentication) {
        return service.evaluateAi(authentication.getName(), id);
    }

    @PostMapping("/{id}/request-info")
    public ApplicationResponse requestInfo(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationNoteRequest request,
            Authentication authentication) {
        return service.requestInfo(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/decision")
    public ApplicationResponse decide(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationDecisionRequest request,
            Authentication authentication) {
        return service.decide(authentication.getName(), id, request);
    }
}
