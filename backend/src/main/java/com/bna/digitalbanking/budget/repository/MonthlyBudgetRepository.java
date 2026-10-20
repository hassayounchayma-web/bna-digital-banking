package com.bna.digitalbanking.budget.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import com.bna.digitalbanking.account.entity.TransactionCategory;
import com.bna.digitalbanking.budget.entity.MonthlyBudget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, Long> {
    List<MonthlyBudget> findByCustomerIdAndMonth(Long customerId, LocalDate month);

    Optional<MonthlyBudget> findByCustomerIdAndMonthAndCategory(
        Long customerId,
        LocalDate month,
        TransactionCategory category);
}
