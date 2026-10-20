package com.bna.digitalbanking.budget.controller;

import com.bna.digitalbanking.budget.dto.BudgetResponse;
import com.bna.digitalbanking.budget.dto.MonthlyBudgetRequest;
import com.bna.digitalbanking.budget.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {
    private final BudgetService service;

    public BudgetController(BudgetService service) {
        this.service = service;
    }

    @GetMapping
    public BudgetResponse getMonth(
            @RequestParam String month,
            Authentication authentication) {
        return service.getMonth(authentication.getName(), month);
    }

    @PutMapping
    public BudgetResponse saveLimit(
            @Valid @RequestBody MonthlyBudgetRequest request,
            Authentication authentication) {
        return service.saveLimit(authentication.getName(), request);
    }
}
