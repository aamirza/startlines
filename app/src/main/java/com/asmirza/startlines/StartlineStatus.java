package com.asmirza.startlines;

public enum StartlineStatus {
    X(-1) {
        @Override
        public String toString() {
            return "X";
        }
    },
    DEFAULT(0) {
        @Override
        public String toString() {
            return "0";
        }
    },
    COMPLETE(1) {
        @Override
        public String toString() {
            return "1";
        }
    };

    private final int value;

    StartlineStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
