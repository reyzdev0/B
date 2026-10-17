package com.savequest.app.domain;

/**
 * Total EXP untuk mencapai level L = 50 * (L-1) * L
 * Lv1 = 0, Lv2 = 100, Lv3 = 300, Lv4 = 600, Lv5 = 1000, ...
 */
public final class LevelSystem {
    private LevelSystem() {}

    public static long expForLevel(int level) {
        return 50L * (level - 1) * level;
    }

    public static int levelFor(long totalExp) {
        int level = 1;
        while (totalExp >= expForLevel(level + 1)) level++;
        return level;
    }

    /** Progress di dalam level saat ini, skala 0..1000 (untuk ProgressBar). */
    public static int progressPermille(long totalExp) {
        int level = levelFor(totalExp);
        long start = expForLevel(level);
        long next = expForLevel(level + 1);
        return (int) ((totalExp - start) * 1000 / (next - start));
    }

    public static String title(int level) {
        if (level >= 20) return "Legenda Tabungan";
        if (level >= 10) return "Master Saver";
        if (level >= 5) return "Penabung Veteran";
        if (level >= 3) return "Penabung Rajin";
        return "Pemula";
    }


    /** Tier warna bingkai level: 1 perunggu, 2 perak, 3 emas, 4 platinum, 5 diamond. */
    public static int tier(int level) {
        if (level >= 35) return 5;
        if (level >= 20) return 4;
        if (level >= 10) return 3;
        if (level >= 5) return 2;
        return 1;
    }
}
