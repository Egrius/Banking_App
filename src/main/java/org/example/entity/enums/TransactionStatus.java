package org.example.entity.enums;

public enum TransactionStatus {

    PENDING("PENDING"), SUCCESS("SUCCESS"), FAILED("FAILED");

    private final String abbreviation;

    TransactionStatus(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }
}
