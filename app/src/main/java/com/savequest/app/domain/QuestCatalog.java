package com.savequest.app.domain;

/** Template quest. Instance harian/mingguan dibuat otomatis oleh repository. */
public final class QuestCatalog {
    private QuestCatalog() {}

    public static final class Template {
        public final String code, title, rewardItem;
        public final long target;
        public final int rewardExp;
        public final boolean weekly;

        Template(String code, String title, long target, int rewardExp, boolean weekly, String rewardItem) {
            this.code = code;
            this.title = title;
            this.target = target;
            this.rewardExp = rewardExp;
            this.weekly = weekly;
            this.rewardItem = rewardItem;
        }
    }

    public static final Template[] ALL = {
            new Template("D5K", "Nabung Rp5.000 hari ini", 5_000, 50, false, ItemCatalog.BANDAGE),
            new Template("D20K", "Nabung Rp20.000 hari ini", 20_000, 150, false, ItemCatalog.EXP_POTION),
            new Template("W100K", "Nabung Rp100.000 minggu ini", 100_000, 300, true, ItemCatalog.COSMETIC_CRATE)
    };

    /** Item bonus yang ikut diberikan saat quest diklaim (null jika tidak ada). */
    public static String rewardItemFor(String code) {
        for (Template t : ALL) if (t.code.equals(code)) return t.rewardItem;
        return null;
    }
}
