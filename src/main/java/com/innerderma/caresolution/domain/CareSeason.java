package com.innerderma.caresolution.domain;

import java.time.LocalDate;

public enum CareSeason {
    SPRING, SUMMER, AUTUMN, WINTER;

    public static CareSeason from(LocalDate date) {
        return switch (date.getMonthValue()) {
            case 3, 4, 5 -> SPRING;
            case 6, 7, 8 -> SUMMER;
            case 9, 10, 11 -> AUTUMN;
            default -> WINTER;
        };
    }
}
