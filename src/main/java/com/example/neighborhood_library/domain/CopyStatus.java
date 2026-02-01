package com.example.neighborhood_library.domain;

public enum CopyStatus {
    AVAILABLE,
    RESERVED,
    LOANED,
    UNAVAILABLE;

    public String getPlLabel() {
        return switch (this) {
            case AVAILABLE -> "DOSTĘPNY";
            case LOANED -> "WYPOŻYCZONY";
            case RESERVED -> "ZAREZERWOWANY";
            case UNAVAILABLE -> "NIEDOSTĘPNY";
        };
    }
}
