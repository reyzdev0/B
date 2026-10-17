package com.savequest.app.domain;

/** Koin: mata uang toko, didapat dari menabung. 1 koin per Rp1.000 (minimal 1). */
public final class EconomySystem {
    private EconomySystem() {}

    public static long coinsForDeposit(long amount) {
        return amount <= 0 ? 0 : Math.max(1, amount / 1000);
    }

    public static long questCoins(int rewardExp) {
        return Math.max(1, rewardExp / 5);
    }
}
