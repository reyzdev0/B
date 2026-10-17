package com.savequest.app.domain;

/** Pembulatan kembalian: belanja 47.300 dibulatkan ke 1.000 -> sisihkan 700. */
public final class RoundUp {
    private RoundUp() {}

    public static long change(long spent, long step) {
        if (spent <= 0 || step <= 0) return 0;
        long remainder = spent % step;
        return remainder == 0 ? 0 : step - remainder;
    }
}
