package com.bna.digitalbanking.credit.application.controller;

import java.util.List;
import com.bna.digitalbanking.credit.application.dto.ApplicationNoteRequest;
import com.bna.digitalbanking.credit.application.dto.ApplicationRequest;
import com.bna.digitalbanking.credit.application.dto.ApplicationResponse;
import com.bna.digitalbanking.credit.application.service.CreditApplicationService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/applications")
public class CreditApplicationController {
    private final CreditApplicationService service;

    public CreditApplicationController(CreditApplicationService service) {
        this.service = service;
    }

    @PostMapping
    public ApplicationResponse create(@Valid @RequestBody ApplicationRequest request, Authentication authentication) {
        return service.createDraft(authentication.getName(), request);
    }

    @GetMapping
    public List<ApplicationResponse> list(Authentication authentication) {
        return service.listOwn(authentication.getName());
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@PathVariable Long id, Authentication authentication) {
        return service.getOwn(authentication.getName(), id);
    }

    @PutMapping("/{id}")
    public ApplicationResponse update(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationRequest request,
            Authentication authentication) {
        return service.updateDraft(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/submit")
    public ApplicationResponse submit(@PathVariable Long id, Authentication authentication) {
        return service.submit(authentication.getName(), id);
    }

    @PostMapping("/{id}/provide-info")
    public ApplicationResponse provideInfo(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationNoteRequest request,
            Authentication authentication) {
        return service.provideInfo(authentication.getName(), id, request);
    }

    @PostMapping("/{id}/withdraw")
    public ApplicationResponse withdraw(@PathVariable Long id, Authentication authentication) {
        return service.withdraw(authentication.getName(), id);
    }
}
