package com.bna.digitalbanking.budget.service;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.bna.digitalbanking.account.entity.AccountTransactionType;
import com.bna.digitalbanking.account.entity.TransactionCategory;
import com.bna.digitalbanking.account.repository.AccountTransactionRepository;
import com.bna.digitalbanking.budget.dto.BudgetCategoryResponse;
import com.bna.digitalbanking.budget.dto.BudgetResponse;
import com.bna.digitalbanking.budget.dto.MonthlyBudgetRequest;
import com.bna.digitalbanking.budget.entity.MonthlyBudget;
import com.bna.digitalbanking.budget.repository.MonthlyBudgetRepository;
import com.bna.digitalbanking.identity.UserEntity;
import com.bna.digitalbanking.identity.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class BudgetService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final MonthlyBudgetRepository budgets;
    private final AccountTransactionRepository transactions;
    private final UserRepository users;

    public BudgetService(
            MonthlyBudgetRepository budgets,
            AccountTransactionRepository transactions,
            UserRepository users) {
        this.budgets = budgets;
        this.transactions = transactions;
        this.users = users;
    }

    public BudgetResponse getMonth(String email, String requestedMonth) {
        YearMonth month = parseMonth(requestedMonth);
        UserEntity customer = findUser(email);
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.plusMonths(1).atDay(1);
        Map<TransactionCategory, MonthlyBudget> monthlyBudgets = budgets
            .findByCustomerIdAndMonth(customer.getId(), startDate)
            .stream()
            .collect(Collectors.toMap(MonthlyBudget::getCategory, Function.identity()));

        List<BudgetCategoryResponse> categories = java.util.Arrays.stream(TransactionCategory.values())
            .filter(TransactionCategory::isBudgetedExpense)
            .map(category -> categoryResponse(
                customer.getId(), category, monthlyBudgets.get(category), startDate, endDate))
            .toList();
        BigDecimal totalIncome = transactions.sumTotalForBudget(
            customer.getId(), AccountTransactionType.INCOME, startDate, endDate);
        BigDecimal totalExpenses = transactions.sumTotalForBudget(
            customer.getId(), AccountTransactionType.EXPENSE, startDate, endDate);
        return new BudgetResponse(
            month,
            totalIncome == null ? ZERO : totalIncome,
            totalExpenses == null ? ZERO : totalExpenses,
            categories);
    }

    public BudgetResponse saveLimit(String email, MonthlyBudgetRequest request) {
        UserEntity customer = findUser(email);
        if (!request.category().isBudgetedExpense()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Un objectif budgétaire ne peut être défini que pour une catégorie de dépense.");
        }
        LocalDate month = request.month().atDay(1);
        MonthlyBudget budget = budgets.findByCustomerIdAndMonthAndCategory(
                customer.getId(), month, request.category())
            .map(existing -> {
                existing.updateLimit(request.monthlyLimit());
                return existing;
            })
            .orElseGet(() -> new MonthlyBudget(customer, month, request.category(), request.monthlyLimit()));
        budgets.save(budget);
        return getMonth(email, request.month().toString());
    }

    private BudgetCategoryResponse categoryResponse(
            Long customerId,
            TransactionCategory category,
            MonthlyBudget budget,
            LocalDate startDate,
            LocalDate endDate) {
        BigDecimal spent = transactions.sumForBudget(
            customerId, AccountTransactionType.EXPENSE, category, startDate, endDate);
        if (spent == null) spent = ZERO;
        BigDecimal limit = budget == null ? null : budget.getMonthlyLimit();
        BigDecimal remaining = limit == null ? null : limit.subtract(spent);
        return new BudgetCategoryResponse(
            category.name(),
            category.getLabel(),
            limit,
            spent,
            remaining,
            remaining != null && remaining.signum() < 0);
    }

    private YearMonth parseMonth(String value) {
        try {
            return YearMonth.parse(value);
        } catch (DateTimeException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Le mois doit respecter le format AAAA-MM.",
                exception);
        }
    }

    private UserEntity findUser(String email) {
        return users.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
    }
}
