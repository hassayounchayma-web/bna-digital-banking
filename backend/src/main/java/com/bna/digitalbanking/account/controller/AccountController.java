package com.bna.digitalbanking.account.controller;

import java.util.List;
import com.bna.digitalbanking.account.dto.AccountOperationRequest;
import com.bna.digitalbanking.account.dto.AccountResponse;
import com.bna.digitalbanking.account.dto.AccountTransactionResponse;
import com.bna.digitalbanking.account.dto.TransferRequest;
import com.bna.digitalbanking.account.dto.TransactionCategoryResponse;
import com.bna.digitalbanking.account.service.AccountService;
import com.bna.digitalbanking.account.service.AccountService.TransferResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    public List<AccountResponse> list(Authentication authentication) {
        return service.listAccounts(authentication.getName());
    }

    @GetMapping("/categories")
    public List<TransactionCategoryResponse> categories() {
        return service.categories();
    }

    @GetMapping("/{accountId}/transactions")
    public List<AccountTransactionResponse> transactions(
            @PathVariable Long accountId,
            Authentication authentication) {
        return service.listTransactions(authentication.getName(), accountId);
    }

    @PostMapping("/{accountId}/transactions")
    public AccountTransactionResponse recordOperation(
            @PathVariable Long accountId,
            @Valid @RequestBody AccountOperationRequest request,
            Authentication authentication) {
        return service.recordOperation(authentication.getName(), accountId, request);
    }

    @PostMapping("/transfers")
    public TransferResponse transfer(
            @Valid @RequestBody TransferRequest request,
            Authentication authentication) {
        return service.transfer(authentication.getName(), request);
    }
}
