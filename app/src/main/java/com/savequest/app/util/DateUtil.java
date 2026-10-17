package com.savequest.app.util;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Locale;

public final class DateUtil {
    private DateUtil() {}

    public static long today() {
        return LocalDate.now().toEpochDay();
    }

    public static long weekStart(long day) {
        return LocalDate.ofEpochDay(day)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay();
    }

    public static long monthStart() {
        return LocalDate.now().withDayOfMonth(1).toEpochDay();
    }

    public static long monthEnd() {
        LocalDate d = LocalDate.now();
        return d.withDayOfMonth(d.lengthOfMonth()).toEpochDay();
    }

    public static long dayMillis(long epochDay) {
        return LocalDate.ofEpochDay(epochDay).atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli();
    }

    public static String time(int h, int m) {
        return String.format(Locale.US, "%02d:%02d", h, m);
    }

    public static String formatDateTime(long millis) {
        return new SimpleDateFormat("dd MMM, HH:mm", new Locale("id", "ID")).format(new Date(millis));
    }

    public static String formatDate(long millis) {
        return new SimpleDateFormat("dd MMM yyyy", new Locale("id", "ID")).format(new Date(millis));
    }
}
