package com.bna.digitalbanking.account.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import com.bna.digitalbanking.account.entity.AccountTransaction;
import com.bna.digitalbanking.account.entity.AccountTransactionType;
import com.bna.digitalbanking.account.entity.AccountType;
import com.bna.digitalbanking.account.entity.TransactionCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {
    List<AccountTransaction> findByAccountIdAndAccountCustomerEmailOrderByOccurredAtDescCreatedAtDesc(
        Long accountId,
        String email);

    @Query("""
        select distinct tx.occurredAt
        from AccountTransaction tx
        join tx.account account
        where account.customer.id = :customerId
          and tx.type = :type
          and tx.category = :category
          and tx.occurredAt >= :startDate
          and tx.occurredAt < :endDate
        """)
    List<LocalDate> findObservedTransactionDates(
        @Param("customerId") Long customerId,
        @Param("type") AccountTransactionType type,
        @Param("category") TransactionCategory category,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("""
        select distinct tx.occurredAt
        from AccountTransaction tx
        join tx.account account
        where account.customer.id = :customerId
          and account.type = :accountType
          and tx.type = :type
          and tx.transferReference is not null
          and tx.occurredAt >= :startDate
          and tx.occurredAt < :endDate
        """)
    List<LocalDate> findObservedTransferDates(
        @Param("customerId") Long customerId,
        @Param("accountType") AccountType accountType,
        @Param("type") AccountTransactionType type,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("""
        select coalesce(sum(tx.amount), 0)
        from AccountTransaction tx
        join tx.account account
        where account.customer.id = :customerId
          and tx.type = :type
          and tx.category = :category
          and tx.occurredAt >= :startDate
          and tx.occurredAt < :endDate
        """)
    BigDecimal sumForBudget(
        @Param("customerId") Long customerId,
        @Param("type") AccountTransactionType type,
        @Param("category") TransactionCategory category,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);

    @Query("""
        select coalesce(sum(tx.amount), 0)
        from AccountTransaction tx
        join tx.account account
        where account.customer.id = :customerId
          and tx.type = :type
          and tx.occurredAt >= :startDate
          and tx.occurredAt < :endDate
        """)
    BigDecimal sumTotalForBudget(
        @Param("customerId") Long customerId,
        @Param("type") AccountTransactionType type,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
