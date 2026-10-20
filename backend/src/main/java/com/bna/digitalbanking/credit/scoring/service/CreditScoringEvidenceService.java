package com.bna.digitalbanking.credit.scoring.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Set;
import java.util.SplittableRandom;
import com.bna.digitalbanking.account.entity.AccountTransactionType;
import com.bna.digitalbanking.account.entity.AccountType;
import com.bna.digitalbanking.account.entity.TransactionCategory;
import com.bna.digitalbanking.account.repository.AccountTransactionRepository;
import com.bna.digitalbanking.identity.UserEntity;
import org.springframework.stereotype.Service;

@Service
public class CreditScoringEvidenceService {
    private static final int STABILITY_LOOKBACK_MONTHS = 24;
    private static final int SAVINGS_LOOKBACK_MONTHS = 12;
    private static final long DEMO_REPAYMENT_SEED = 0x424E4144454D4FL;

    private final AccountTransactionRepository transactions;

    public CreditScoringEvidenceService(AccountTransactionRepository transactions) {
        this.transactions = transactions;
    }

    public Evidence detect(UserEntity customer) {
        LocalDate today = LocalDate.now();
        LocalDate firstStabilityDate = today.withDayOfMonth(1)
            .minusMonths(STABILITY_LOOKBACK_MONTHS - 1L);
        LocalDate firstSavingsDate = today.withDayOfMonth(1)
            .minusMonths(SAVINGS_LOOKBACK_MONTHS - 1L);
        LocalDate endDate = today.plusDays(1);

        Set<YearMonth> salaryMonths = new HashSet<>(transactions.findObservedTransactionDates(
            customer.getId(),
            AccountTransactionType.INCOME,
            TransactionCategory.SALARY,
            firstStabilityDate,
            endDate).stream().map(YearMonth::from).toList());
        Integer stabilityMonths = salaryMonths.isEmpty()
            ? null
            : consecutiveSalaryMonths(salaryMonths, YearMonth.from(today), STABILITY_LOOKBACK_MONTHS);

        Set<YearMonth> savingsMonths = new HashSet<>(transactions.findObservedTransferDates(
            customer.getId(),
            AccountType.SAVINGS,
            AccountTransactionType.TRANSFER_IN,
            firstSavingsDate,
            endDate).stream().map(YearMonth::from).toList());
        Integer regularSavingsMonths = savingsMonths.isEmpty()
            ? null
            : savingsMonths.size();

        return new Evidence(stabilityMonths, generateDemoLatePayments(customer.getId()), regularSavingsMonths);
    }

    private int consecutiveSalaryMonths(Set<YearMonth> salaryMonths, YearMonth currentMonth, int maximumMonths) {
        int consecutiveMonths = 0;
        for (int offset = 0; offset < maximumMonths; offset++) {
            if (!salaryMonths.contains(currentMonth.minusMonths(offset))) {
                break;
            }
            consecutiveMonths++;
        }
        return consecutiveMonths;
    }

    private int generateDemoLatePayments(Long customerId) {
        SplittableRandom random = new SplittableRandom(customerId ^ DEMO_REPAYMENT_SEED);
        int latePayments = 0;
        for (int month = 0; month < SAVINGS_LOOKBACK_MONTHS; month++) {
            if (random.nextInt(5) == 0) {
                latePayments++;
            }
        }
        return latePayments;
    }

    public record Evidence(
        Integer incomeStabilityMonths,
        Integer latePaymentsLast12Months,
        Integer savingsRegularityMonthsLast12Months
    ) {
    }
}
