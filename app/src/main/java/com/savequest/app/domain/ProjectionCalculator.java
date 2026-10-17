package com.savequest.app.domain;

/** Kalkulator proyeksi: berapa lama target tercapai dengan setoran rutin. */
public final class ProjectionCalculator {
    private ProjectionCalculator() {}

    public enum Frequency {
        DAILY(1, "hari"), WEEKLY(7, "minggu"), MONTHLY(30, "bulan");

        public final int days;
        public final String label;

        Frequency(int days, String label) {
            this.days = days;
            this.label = label;
        }
    }

    /** Jumlah periode yang dibutuhkan. 0 = sudah tercapai, -1 = setoran tidak valid. */
    public static long periodsNeeded(long target, long saved, long perPeriod) {
        long remain = target - saved;
        if (remain <= 0) return 0;
        if (perPeriod <= 0) return -1;
        return (remain + perPeriod - 1) / perPeriod;
    }
}
