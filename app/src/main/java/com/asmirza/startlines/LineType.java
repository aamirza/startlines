package com.asmirza.startlines;

public enum LineType {
    STARTLINE("startline"),
    FUNLINE("funline");

    private final String value;

    LineType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static LineType fromString(String text) {
        for (LineType type : LineType.values()) {
            if (type.value.equalsIgnoreCase(text)) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum constant with value " + text);
    }
}
