package org.example.entity.enums;

public enum Status {
    ACTIVE("ACTIVE"), BLOCKED("BLOCKED"), CLOSED("CLOSED"),
    PENDING("PENDING"), SUCCESS("SUCCESS"), FAILED("FAILED");

    private final String abbreviation;

    Status(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }
}
