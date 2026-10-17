package com.savequest.app.domain;

/**
 * EXP setoran = round( base x multiplier streak x buff roda x faktor HP )
 *   base = 10 + min(90, nominal / 1000)   (10..100 EXP)
 */
public final class ExpCalculator {
    private ExpCalculator() {}

    public static int base(long amount) {
        if (amount <= 0) return 0;
        return 10 + (int) Math.min(90, amount / 1000);
    }

    public static int forDeposit(long amount, int streakDays) {
        return compute(amount, StreakSystem.multiplier(streakDays), 1.0, 1.0);
    }

    public static int compute(long amount, double streakMult, double buff, double hpMult) {
        return (int) Math.round(base(amount) * streakMult * buff * hpMult);
    }
}
