package com.medtrack.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String now() {
        return LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    public static String today() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    public static long daysUntil(String targetDateStr) {
        if (targetDateStr == null || targetDateStr.isBlank()) return 0;
        try {
            LocalDate targetDate = LocalDate.parse(targetDateStr.substring(0, 10), DATE_FORMATTER);
            return ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String getExpiryStatus(long daysRemaining) {
        if (daysRemaining < 0) {
            return "EXPIRED";
        } else if (daysRemaining <= 30) {
            return "CRITICAL";
        } else if (daysRemaining <= 90) {
            return "WARNING";
        } else {
            return "SAFE";
        }
    }
}
