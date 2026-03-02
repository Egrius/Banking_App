package org.example.entity.enums;

public enum Status {
    ACTIVE("ACTIVE"), BLOCKED("BLOCKED"), CLOSED("CLOSED");

    private final String abbreviation;

    Status(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }
}
