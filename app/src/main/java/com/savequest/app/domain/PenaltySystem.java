package com.savequest.app.domain;

import java.util.Random;

/** Penalti menarik Emergency Vault sebelum tenggat. */
public final class PenaltySystem {
    private PenaltySystem() {}

    public static final int EMERGENCY_HP = 10;
    public static final int EMERGENCY_DEFAULT_LOCK_DAYS = 90;

    /** EXP yang hilang: 25 + nominal/2000, maksimal 300. */
    public static int emergencyExp(long amount) {
        return (int) Math.min(300, 25 + Math.max(0, amount) / 2000);
    }

    private static final String[] MOTIVATION = {
            "Brankas darurat dibuat untuk keadaan darurat sungguhan. Apakah ini benar-benar mendesak?",
            "Tunggu 10 menit dulu. Banyak keinginan hilang sendiri, tapi dana darurat butuh bertahun-tahun.",
            "Dirimu yang dulu menabung ini supaya dirimu yang sekarang aman. Jangan khianati dia.",
            "Kalau ini cuma keinginan, bukan kebutuhan, coba tunda dulu satu hari.",
            "Setiap rupiah di brankas ini adalah napas panjang saat badai datang."
    };

    public static String motivation(Random r) {
        return MOTIVATION[r.nextInt(MOTIVATION.length)];
    }
}
