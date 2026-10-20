package com.bna.digitalbanking.account.service;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.DecimalMin;

@Component
@Validated
@ConfigurationProperties(prefix = "app.accounts.initial-balances")
public class AccountSeedProperties {
    @DecimalMin("0")
    private BigDecimal current = new BigDecimal("2000");

    @DecimalMin("0")
    private BigDecimal savings = new BigDecimal("5000");

    public BigDecimal getCurrent() { return current; }
    public void setCurrent(BigDecimal value) { current = value; }
    public BigDecimal getSavings() { return savings; }
    public void setSavings(BigDecimal value) { savings = value; }
}
