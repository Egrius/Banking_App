package org.example.entity.enums;

public enum AccountType {
    CURRENT("CURRENT"), SAVINGS("SAVINGS"), CREDIT("CREDIT");

    private final String abbreviation;

    AccountType(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }
}
