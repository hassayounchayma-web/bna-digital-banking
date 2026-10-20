package com.bna.digitalbanking.account.entity;

public enum TransactionCategory {
    SALARY("Salaire", false),
    OTHER_INCOME("Autres revenus", false),
    FOOD("Alimentation", true),
    HOUSING("Logement", true),
    TRANSPORT("Transport", true),
    HEALTH("Santé", true),
    EDUCATION("Éducation", true),
    UTILITIES("Factures", true),
    LEISURE("Loisirs", true),
    OTHER("Autre", true);

    private final String label;
    private final boolean budgetedExpense;

    TransactionCategory(String label, boolean budgetedExpense) {
        this.label = label;
        this.budgetedExpense = budgetedExpense;
    }

    public String getLabel() { return label; }
    public boolean isBudgetedExpense() { return budgetedExpense; }
}
