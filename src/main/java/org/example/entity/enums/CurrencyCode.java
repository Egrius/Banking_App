package org.example.entity.enums;

public enum CurrencyCode {
    RUB("RUB"), EUR("EUR" ), US("US");

    private final String abbreviation;

    CurrencyCode(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }
}
