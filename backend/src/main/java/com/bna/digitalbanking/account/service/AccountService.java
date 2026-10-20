package com.bna.digitalbanking.account.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import com.bna.digitalbanking.account.dto.AccountOperationRequest;
import com.bna.digitalbanking.account.dto.AccountResponse;
import com.bna.digitalbanking.account.dto.AccountTransactionResponse;
import com.bna.digitalbanking.account.dto.TransactionCategoryResponse;
import com.bna.digitalbanking.account.dto.TransferRequest;
import com.bna.digitalbanking.account.entity.AccountTransaction;
import com.bna.digitalbanking.account.entity.AccountTransactionType;
import com.bna.digitalbanking.account.entity.AccountType;
import com.bna.digitalbanking.account.entity.BankAccount;
import com.bna.digitalbanking.account.entity.TransactionCategory;
import com.bna.digitalbanking.account.repository.AccountTransactionRepository;
import com.bna.digitalbanking.account.repository.BankAccountRepository;
import com.bna.digitalbanking.identity.UserEntity;
import com.bna.digitalbanking.identity.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AccountService {
    private final BankAccountRepository accounts;
    private final AccountTransactionRepository transactions;
    private final UserRepository users;
    private final AccountSeedProperties seedProperties;

    public AccountService(
            BankAccountRepository accounts,
            AccountTransactionRepository transactions,
            UserRepository users,
            AccountSeedProperties seedProperties) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.users = users;
        this.seedProperties = seedProperties;
    }

    public List<AccountResponse> listAccounts(String email) {
        UserEntity customer = findUser(email);
        seedMissingAccounts(customer);
        return accounts.findByCustomerEmailOrderByTypeAsc(email).stream()
            .map(AccountResponse::from)
            .toList();
    }

    public List<TransactionCategoryResponse> categories() {
        return java.util.Arrays.stream(TransactionCategory.values())
            .map(TransactionCategoryResponse::from)
            .toList();
    }

    public List<AccountTransactionResponse> listTransactions(String email, Long accountId) {
        findOwnedAccount(email, accountId);
        return transactions
            .findByAccountIdAndAccountCustomerEmailOrderByOccurredAtDescCreatedAtDesc(accountId, email)
            .stream()
            .map(AccountTransactionResponse::from)
            .toList();
    }

    public AccountTransactionResponse recordOperation(
            String email,
            Long accountId,
            AccountOperationRequest request) {
        BankAccount account = findOwnedAccountForUpdate(email, accountId);
        validateCategory(request.type(), request.category());
        if (request.type() == AccountOperationRequest.OperationType.EXPENSE
                && account.getCurrentBalance().compareTo(request.amount()) < 0) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Solde fictif insuffisant pour enregistrer cette dépense.");
        }
        AccountTransactionType type = request.type() == AccountOperationRequest.OperationType.INCOME
            ? AccountTransactionType.INCOME
            : AccountTransactionType.EXPENSE;
        if (type == AccountTransactionType.INCOME) {
            account.applyIncome(request.amount());
        } else {
            account.applyExpense(request.amount());
        }
        AccountTransaction transaction = transactions.save(new AccountTransaction(
            account,
            type,
            request.category(),
            request.amount(),
            request.occurredAt(),
            normalizeDescription(request.description()),
            null));
        return AccountTransactionResponse.from(transaction);
    }

    public TransferResponse transfer(String email, TransferRequest request) {
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Les comptes source et destinataire doivent être différents.");
        }
        Long firstId = Math.min(request.sourceAccountId(), request.destinationAccountId());
        Long secondId = Math.max(request.sourceAccountId(), request.destinationAccountId());
        BankAccount first = findOwnedAccountForUpdate(email, firstId);
        BankAccount second = findOwnedAccountForUpdate(email, secondId);
        BankAccount source = first.getId().equals(request.sourceAccountId()) ? first : second;
        BankAccount destination = first.getId().equals(request.destinationAccountId()) ? first : second;
        if (source.getCurrentBalance().compareTo(request.amount()) < 0) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Solde fictif insuffisant pour effectuer ce virement.");
        }
        String transferReference = UUID.randomUUID().toString();
        String description = normalizeDescription(request.description());
        source.applyExpense(request.amount());
        destination.applyIncome(request.amount());
        AccountTransaction outgoing = transactions.save(new AccountTransaction(
            source,
            AccountTransactionType.TRANSFER_OUT,
            null,
            request.amount(),
            request.occurredAt(),
            description,
            transferReference));
        AccountTransaction incoming = transactions.save(new AccountTransaction(
            destination,
            AccountTransactionType.TRANSFER_IN,
            null,
            request.amount(),
            request.occurredAt(),
            description,
            transferReference));
        return new TransferResponse(
            transferReference,
            AccountTransactionResponse.from(outgoing),
            AccountTransactionResponse.from(incoming),
            AccountResponse.from(source),
            AccountResponse.from(destination));
    }

    private void seedMissingAccounts(UserEntity customer) {
        if (accounts.findByCustomerIdAndType(customer.getId(), AccountType.CURRENT).isEmpty()) {
            accounts.save(new BankAccount(
                customer, AccountType.CURRENT, "Compte courant", seedProperties.getCurrent()));
        }
        if (accounts.findByCustomerIdAndType(customer.getId(), AccountType.SAVINGS).isEmpty()) {
            accounts.save(new BankAccount(
                customer, AccountType.SAVINGS, "Compte épargne", seedProperties.getSavings()));
        }
    }

    private BankAccount findOwnedAccount(String email, Long id) {
        return accounts.findByIdAndCustomerEmail(id, email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable."));
    }

    private BankAccount findOwnedAccountForUpdate(String email, Long id) {
        return accounts.findOwnedForUpdate(id, email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable."));
    }

    private UserEntity findUser(String email) {
        return users.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
    }

    private void validateCategory(
            AccountOperationRequest.OperationType type,
            TransactionCategory category) {
        boolean validCategory = type == AccountOperationRequest.OperationType.INCOME
            ? !category.isBudgetedExpense()
            : category.isBudgetedExpense();
        if (!validCategory) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La catégorie sélectionnée ne correspond pas au type de l’opération.");
        }
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.strip();
    }

    public record TransferResponse(
        String transferReference,
        AccountTransactionResponse outgoing,
        AccountTransactionResponse incoming,
        AccountResponse sourceAccount,
        AccountResponse destinationAccount
    ) {
    }
}
