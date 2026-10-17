package com.savequest.app.domain;

import java.util.Locale;

/**
 * Streak = hari berturut-turut menabung. Multiplier EXP:
 *   0-2 hari = 1.0x | 3-6 = 1.2x | 7-13 = 1.5x | 14-29 = 1.8x | 30+ = 2.0x
 * Perisai Streak menutup 1 hari terlewat, Gulungan Pelindung sampai 3 hari.
 */
public final class StreakSystem {
    private StreakSystem() {}

    private static final int[] MILESTONES = {3, 7, 14, 30};

    public static double multiplier(int streak) {
        if (streak >= 30) return 2.0;
        if (streak >= 14) return 1.8;
        if (streak >= 7) return 1.5;
        if (streak >= 3) return 1.2;
        return 1.0;
    }

    /** Jumlah hari yang terlewat sejak setoran terakhir (0 = hari ini atau kemarin). -1 = belum pernah. */
    public static int gapDays(long lastDay, long today) {
        if (lastDay < 0) return -1;
        return (int) Math.max(0, today - lastDay - 1);
    }

    /** Jumlah hari terlewat yang bisa ditutup item di ransel. */
    public static int cover(int shields, int scrolls) {
        if (scrolls > 0) return 3;
        if (shields > 0) return 1;
        return 0;
    }

    /** Streak yang masih hidup (tanpa memperhitungkan item pelindung). */
    public static int effective(int streak, long lastDay, long today) {
        return effective(streak, lastDay, today, 0);
    }

    /** Streak yang masih hidup, dengan memperhitungkan item pelindung di ransel. */
    public static int effective(int streak, long lastDay, long today, int cover) {
        int gap = gapDays(lastDay, today);
        if (gap < 0) return 0;
        return gap <= cover ? streak : 0;
    }

    /** Streak yang akan berlaku jika user menabung hari ini. */
    public static int projected(int streak, long lastDay, long today) {
        return projected(streak, lastDay, today, 0);
    }

    public static int projected(int streak, long lastDay, long today, int cover) {
        if (lastDay == today) return streak;
        int gap = gapDays(lastDay, today);
        if (gap < 0) return 1;
        return gap <= cover ? streak + 1 : 1;
    }

    /** Milestone berikutnya, atau -1 jika sudah maksimum. */
    public static int nextMilestone(int streak) {
        for (int m : MILESTONES) if (streak < m) return m;
        return -1;
    }

    public static String label(double multiplier) {
        return String.format(Locale.US, "%.1fx", multiplier);
    }
}
