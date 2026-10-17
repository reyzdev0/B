package com.savequest.app.domain;

import java.util.Random;

/** Roda keberuntungan harian: 8 hadiah (EXP, koin, buff, dan item) dengan bobot peluang berbeda. */
public final class SpinSystem {
    private SpinSystem() {}

    public static final int EXP = 0, COINS = 1, BUFF = 2, HEAL = 3, ITEM = 4;

    public static final class Prize {
        public final int type;
        public final double value;
        /** Khusus BUFF: berlaku sampai hari ini + days. */
        public final int days;
        /** Khusus ITEM: id item yang diberikan (jumlah = value). */
        public final String itemId;
        public final String label;
        public final int weight;

        Prize(int type, double value, int days, String itemId, String label, int weight) {
            this.type = type;
            this.value = value;
            this.days = days;
            this.itemId = itemId;
            this.label = label;
            this.weight = weight;
        }
    }

    public static final Prize[] PRIZES = {
            new Prize(EXP, 20, 0, null, "+20 EXP", 22),
            new Prize(COINS, 30, 0, null, "+30 Koin", 18),
            new Prize(EXP, 50, 0, null, "+50 EXP", 14),
            new Prize(ITEM, 1, 0, ItemCatalog.BANDAGE, "Perban", 14),
            new Prize(BUFF, 1.5, 1, null, "1.5x EXP", 10),
            new Prize(COINS, 100, 0, null, "+100 Koin", 8),
            new Prize(ITEM, 1, 0, ItemCatalog.EXP_POTION, "Ramuan EXP", 8),
            new Prize(ITEM, 1, 0, ItemCatalog.STREAK_SHIELD, "Perisai", 6)
    };

    public static String[] labels() {
        String[] l = new String[PRIZES.length];
        for (int i = 0; i < l.length; i++) l[i] = PRIZES[i].label;
        return l;
    }

    public static int pick(Random r) {
        int total = 0;
        for (Prize p : PRIZES) total += p.weight;
        int roll = r.nextInt(total);
        for (int i = 0; i < PRIZES.length; i++) {
            roll -= PRIZES[i].weight;
            if (roll < 0) return i;
        }
        return 0;
    }
}
