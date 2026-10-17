package com.savequest.app.domain;

/**
 * HP avatar (0-100) sebagai denda disiplin:
 *  - Tiap hari menabung yang terlewat: -8 HP (maks -40 per pengecekan).
 *  - Penarikan berlebihan: >= 50% saldo vault -15 HP, >= 25% saldo -5 HP.
 *  - Setor +5 HP, klaim quest +10 HP.
 *  - HP rendah menurunkan EXP: >=50 normal | 20-49 x0.9 | <20 x0.75.
 */
public final class HpSystem {
    private HpSystem() {}

    public static final int MAX = 100;

    public static int missedPenalty(int missedDays) {
        return Math.min(40, Math.max(0, missedDays) * 8);
    }

    public static int withdrawPenalty(long amount, long balanceBefore) {
        if (balanceBefore <= 0 || amount <= 0) return 0;
        if (amount * 2 >= balanceBefore) return 15;
        if (amount * 4 >= balanceBefore) return 5;
        return 0;
    }

    public static int healOnDeposit() { return 5; }

    public static int healOnClaim() { return 10; }

    public static double expMultiplier(int hp) {
        if (hp >= 50) return 1.0;
        if (hp >= 20) return 0.9;
        return 0.75;
    }

    public static int color(int hp) {
        if (hp >= 50) return 0xFF43A047;
        if (hp >= 20) return 0xFFFB8C00;
        return 0xFFE53935;
    }

    public static String status(int hp) {
        if (hp >= 50) return "Prima";
        if (hp >= 20) return "Lelah";
        return "Kritis";
    }
}
