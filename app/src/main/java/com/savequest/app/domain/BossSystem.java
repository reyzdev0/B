package com.savequest.app.domain;

/** Boss Battle: target tabungan = HP boss. Setoran = damage. */
public final class BossSystem {
    private BossSystem() {}

    public static final int KIND_COUNT = 3;
    private static final String[] NAMES = {"Slime Boros", "Golem Impulsif", "Naga Utang"};

    public static String kindName(int kind) {
        return NAMES[Math.floorMod(kind, KIND_COUNT)];
    }

    /** Bonus EXP saat boss kalah: 200 + target/5000, maksimal 1000. */
    public static int defeatExp(long target) {
        return (int) Math.min(1000, 200 + Math.max(0, target) / 5000);
    }

    public static long defeatCoins(long target) {
        return Math.min(500, 50 + Math.max(0, target) / 20000);
    }

    /** Sisa HP boss (0..1). */
    public static float hpFraction(long saved, long target) {
        if (target <= 0) return 0f;
        return Math.max(0f, 1f - (float) saved / target);
    }
}
