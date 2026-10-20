package com.bna.digitalbanking.account.repository;

import java.util.List;
import java.util.Optional;
import com.bna.digitalbanking.account.entity.AccountType;
import com.bna.digitalbanking.account.entity.BankAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    List<BankAccount> findByCustomerEmailOrderByTypeAsc(String email);
    Optional<BankAccount> findByCustomerIdAndType(Long customerId, AccountType type);
    Optional<BankAccount> findByIdAndCustomerEmail(Long id, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from BankAccount account where account.id = :id and account.customer.email = :email")
    Optional<BankAccount> findOwnedForUpdate(@Param("id") Long id, @Param("email") String email);
}
