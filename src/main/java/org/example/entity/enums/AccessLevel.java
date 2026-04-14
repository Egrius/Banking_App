package org.example.entity.enums;

public enum AccessLevel {
    ALLOWED("ALLOWED"), NOT_FOUND("NOT_FOUND"), DENIED("DENIED");

    private String abbr;

    AccessLevel(String abbr) {
        this.abbr = abbr;
    }

    public String getAbbr() {
        return abbr;
    }


}
