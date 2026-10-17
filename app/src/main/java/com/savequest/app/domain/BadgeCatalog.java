package com.savequest.app.domain;

import com.savequest.app.R;
import com.savequest.app.data.local.entity.BadgeEntity;
import com.savequest.app.util.Money;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Katalog 57 badge. Tier: 1 Bronze, 2 Silver, 3 Gold, 4 Platinum, 5 Diamond, 6 Rahasia.
 * Setiap badge punya Trigger (apa yang diukur) + threshold (batas yang harus dicapai).
 */
public final class BadgeCatalog {
    private BadgeCatalog() {}

    public static final String CAT_FIRST = "First Step";
    public static final String CAT_STREAK = "Streak";
    public static final String CAT_TARGET = "Target";
    public static final String CAT_MILESTONE = "Milestone";
    public static final String CAT_ECON = "Ekonomi";
    public static final String CAT_ADV = "Petualangan";
    public static final String CAT_SECRET = "Rahasia";

    public static final List<String> CATEGORIES = Arrays.asList(
            CAT_FIRST, CAT_STREAK, CAT_TARGET, CAT_MILESTONE, CAT_ECON, CAT_ADV, CAT_SECRET);

    public static final int TIER_SECRET = 6;
    /** Urutan tampilan saat dikelompokkan per tier. */
    public static final int[] TIERS = {1, 2, 3, 4, 5, TIER_SECRET};

    public static String tierName(int tier) {
        switch (tier) {
            case 1: return "Bronze";
            case 2: return "Silver";
            case 3: return "Gold";
            case 4: return "Platinum";
            case 5: return "Diamond";
            default: return "Rahasia";
        }
    }

    /** Apa yang diukur oleh sebuah badge. */
    public enum Trigger {
        DEPOSITS, STREAK, PEAK_TOTAL, LEVEL, LIFETIME_COINS, VAULTS_DONE, BOSS_KILLS, ITEMS_USED,
        PURCHASES, SPINS, QUEST_CLAIMS, SHIELD_SAVES, MAX_DEPOSIT,
        EVENT_NIGHT, EVENT_MORNING, EVENT_NEW_YEAR, EVENT_SURVIVOR
    }

    public static final class Def {
        public final String code, category, title, description;
        public final int iconRes, tier;
        public final Trigger trigger;
        public final long threshold;
        public final boolean secret;

        Def(String code, String category, String title, String description, int iconRes,
            int tier, Trigger trigger, long threshold) {
            this.code = code;
            this.category = category;
            this.title = title;
            this.description = description;
            this.iconRes = iconRes;
            this.tier = tier;
            this.trigger = trigger;
            this.threshold = threshold;
            this.secret = tier == TIER_SECRET;
        }

        /** Badge dengan angka progres (bukan kejadian khusus). */
        public boolean hasProgress() {
            return threshold > 0;
        }

        public boolean isMoney() {
            return trigger == Trigger.PEAK_TOTAL || trigger == Trigger.MAX_DEPOSIT;
        }

        public String progressText(long metric) {
            long m = Math.min(metric, threshold);
            if (isMoney()) return Money.rp(m) + " / " + Money.rp(threshold);
            return m + " / " + threshold;
        }
    }

    private static final List<Def> LIST = new ArrayList<>();

    private static void d(String code, String cat, String title, String desc, int icon, int tier,
                          Trigger t, long threshold) {
        LIST.add(new Def(code, cat, title, desc, icon, tier, t, threshold));
    }

    static {
        // ---- First Step: jumlah setoran ----
        d("FIRST_STEP", CAT_FIRST, "Langkah Pertama", "Lakukan setoran pertamamu.", R.drawable.ic_flag, 1, Trigger.DEPOSITS, 1);
        d("DEPOSITS_10", CAT_FIRST, "Rajin Setor", "Lakukan 10 kali setoran.", R.drawable.ic_deposit, 1, Trigger.DEPOSITS, 10);
        d("DEPOSITS_50", CAT_FIRST, "Setoran Setia", "Lakukan 50 kali setoran.", R.drawable.ic_deposit, 2, Trigger.DEPOSITS, 50);
        d("DEPOSITS_100", CAT_FIRST, "Seratus Setoran", "Lakukan 100 kali setoran.", R.drawable.ic_deposit, 3, Trigger.DEPOSITS, 100);
        d("DEPOSITS_500", CAT_FIRST, "Mesin Tabungan", "Lakukan 500 kali setoran.", R.drawable.ic_bolt, 4, Trigger.DEPOSITS, 500);
        d("DEPOSITS_1000", CAT_FIRST, "Legenda Setoran", "Lakukan 1.000 kali setoran.", R.drawable.ic_star, 5, Trigger.DEPOSITS, 1000);

        // ---- Streak ----
        d("STREAK_3", CAT_STREAK, "Api Kecil", "Menabung 3 hari berturut-turut.", R.drawable.ic_flame, 1, Trigger.STREAK, 3);
        d("STREAK_7", CAT_STREAK, "Api Konsisten", "Menabung 7 hari berturut-turut.", R.drawable.ic_flame, 2, Trigger.STREAK, 7);
        d("STREAK_14", CAT_STREAK, "Api Membara", "Menabung 14 hari berturut-turut.", R.drawable.ic_flame, 3, Trigger.STREAK, 14);
        d("STREAK_30", CAT_STREAK, "Api Abadi", "Menabung 30 hari berturut-turut.", R.drawable.ic_flame, 4, Trigger.STREAK, 30);
        d("STREAK_100", CAT_STREAK, "Api Dewa", "Menabung 100 hari berturut-turut.", R.drawable.ic_flame, 5, Trigger.STREAK, 100);

        // ---- Target: vault selesai + boss ----
        d("VAULT_DONE", CAT_TARGET, "Target Tercapai", "Selesaikan target 1 vault.", R.drawable.ic_target, 1, Trigger.VAULTS_DONE, 1);
        d("VAULT_3", CAT_TARGET, "Kolektor Target", "Selesaikan target 3 vault.", R.drawable.ic_target, 2, Trigger.VAULTS_DONE, 3);
        d("VAULT_10", CAT_TARGET, "Penakluk Target", "Selesaikan target 10 vault.", R.drawable.ic_trophy, 3, Trigger.VAULTS_DONE, 10);
        d("VAULT_25", CAT_TARGET, "Raja Target", "Selesaikan target 25 vault.", R.drawable.ic_trophy, 4, Trigger.VAULTS_DONE, 25);
        d("BOSS_SLAYER", CAT_TARGET, "Pembasmi Boss", "Kalahkan 1 Boss Battle.", R.drawable.ic_skull, 1, Trigger.BOSS_KILLS, 1);
        d("BOSS_3", CAT_TARGET, "Pemburu Boss", "Kalahkan 3 Boss Battle.", R.drawable.ic_skull, 2, Trigger.BOSS_KILLS, 3);
        d("BOSS_10", CAT_TARGET, "Penumpas Boss", "Kalahkan 10 Boss Battle.", R.drawable.ic_skull, 3, Trigger.BOSS_KILLS, 10);
        d("BOSS_25", CAT_TARGET, "Mimpi Buruk Boss", "Kalahkan 25 Boss Battle.", R.drawable.ic_skull, 4, Trigger.BOSS_KILLS, 25);

        // ---- Milestone: total tabungan, level, setoran besar ----
        d("TOTAL_100K", CAT_MILESTONE, "Seratus Ribu", "Total tabungan mencapai Rp100.000.", R.drawable.ic_coin, 1, Trigger.PEAK_TOTAL, 100_000);
        d("TOTAL_500K", CAT_MILESTONE, "Setengah Juta", "Total tabungan mencapai Rp500.000.", R.drawable.ic_coin, 2, Trigger.PEAK_TOTAL, 500_000);
        d("TOTAL_1M", CAT_MILESTONE, "Jutawan Kecil", "Total tabungan mencapai Rp1.000.000.", R.drawable.ic_coin, 3, Trigger.PEAK_TOTAL, 1_000_000);
        d("TOTAL_5M", CAT_MILESTONE, "Lima Juta", "Total tabungan mencapai Rp5.000.000.", R.drawable.ic_coin, 4, Trigger.PEAK_TOTAL, 5_000_000);
        d("TOTAL_10M", CAT_MILESTONE, "Sultan Tabungan", "Total tabungan mencapai Rp10.000.000.", R.drawable.ic_star, 5, Trigger.PEAK_TOTAL, 10_000_000);
        d("LEVEL_3", CAT_MILESTONE, "Petualang Baru", "Capai Level 3.", R.drawable.ic_bolt, 1, Trigger.LEVEL, 3);
        d("LEVEL_5", CAT_MILESTONE, "Veteran", "Capai Level 5.", R.drawable.ic_bolt, 2, Trigger.LEVEL, 5);
        d("LEVEL_10", CAT_MILESTONE, "Master Saver", "Capai Level 10.", R.drawable.ic_star, 3, Trigger.LEVEL, 10);
        d("LEVEL_20", CAT_MILESTONE, "Legenda", "Capai Level 20.", R.drawable.ic_star, 4, Trigger.LEVEL, 20);
        d("LEVEL_35", CAT_MILESTONE, "Dewa Tabungan", "Capai Level 35.", R.drawable.ic_star, 5, Trigger.LEVEL, 35);
        d("SINGLE_100K", CAT_MILESTONE, "Setoran Besar", "Satu kali setoran minimal Rp100.000.", R.drawable.ic_deposit, 2, Trigger.MAX_DEPOSIT, 100_000);
        d("SINGLE_1M", CAT_MILESTONE, "Setoran Raksasa", "Satu kali setoran minimal Rp1.000.000.", R.drawable.ic_deposit, 3, Trigger.MAX_DEPOSIT, 1_000_000);

        // ---- Ekonomi: koin, belanja, item ----
        d("COINS_100", CAT_ECON, "Pengumpul Koin", "Kumpulkan total 100 koin.", R.drawable.ic_coin, 1, Trigger.LIFETIME_COINS, 100);
        d("COINS_500", CAT_ECON, "Kantong Penuh", "Kumpulkan total 500 koin.", R.drawable.ic_pouch, 2, Trigger.LIFETIME_COINS, 500);
        d("COINS_2000", CAT_ECON, "Bendahara", "Kumpulkan total 2.000 koin.", R.drawable.ic_pouch, 3, Trigger.LIFETIME_COINS, 2000);
        d("COINS_10000", CAT_ECON, "Taipan Koin", "Kumpulkan total 10.000 koin.", R.drawable.ic_chest, 4, Trigger.LIFETIME_COINS, 10_000);
        d("COINS_50000", CAT_ECON, "Raja Koin", "Kumpulkan total 50.000 koin.", R.drawable.ic_chest, 5, Trigger.LIFETIME_COINS, 50_000);
        d("FIRST_PURCHASE", CAT_ECON, "Pembeli Pertama", "Beli item pertamamu di Toko.", R.drawable.ic_shop, 1, Trigger.PURCHASES, 1);
        d("PURCHASES_10", CAT_ECON, "Pelanggan Setia", "Beli 10 item di Toko.", R.drawable.ic_shop, 2, Trigger.PURCHASES, 10);
        d("PURCHASES_50", CAT_ECON, "Langganan VIP", "Beli 50 item di Toko.", R.drawable.ic_shop, 3, Trigger.PURCHASES, 50);
        d("FIRST_ITEM", CAT_ECON, "Pemakai Pertama", "Gunakan item pertamamu dari Ransel.", R.drawable.ic_potion, 1, Trigger.ITEMS_USED, 1);
        d("ITEMS_10", CAT_ECON, "Ahli Ramuan", "Gunakan 10 item.", R.drawable.ic_potion, 2, Trigger.ITEMS_USED, 10);
        d("ITEMS_50", CAT_ECON, "Alkemis", "Gunakan 50 item.", R.drawable.ic_elixir, 3, Trigger.ITEMS_USED, 50);
        d("ITEMS_200", CAT_ECON, "Grand Alkemis", "Gunakan 200 item.", R.drawable.ic_elixir, 4, Trigger.ITEMS_USED, 200);

        // ---- Petualangan: spin, quest, perisai ----
        d("SPIN_1", CAT_ADV, "Putaran Pertama", "Putar roda keberuntungan pertama kali.", R.drawable.ic_spin, 1, Trigger.SPINS, 1);
        d("SPIN_10", CAT_ADV, "Roda Setia", "Putar roda 10 kali.", R.drawable.ic_spin, 2, Trigger.SPINS, 10);
        d("SPIN_50", CAT_ADV, "Raja Roda", "Putar roda 50 kali.", R.drawable.ic_spin, 3, Trigger.SPINS, 50);
        d("SPIN_200", CAT_ADV, "Dewa Keberuntungan", "Putar roda 200 kali.", R.drawable.ic_spin, 4, Trigger.SPINS, 200);
        d("QUEST_1", CAT_ADV, "Penerima Misi", "Klaim hadiah quest pertamamu.", R.drawable.ic_check, 1, Trigger.QUEST_CLAIMS, 1);
        d("QUEST_10", CAT_ADV, "Pemburu Misi", "Klaim 10 hadiah quest.", R.drawable.ic_check, 2, Trigger.QUEST_CLAIMS, 10);
        d("QUEST_50", CAT_ADV, "Veteran Misi", "Klaim 50 hadiah quest.", R.drawable.ic_target, 3, Trigger.QUEST_CLAIMS, 50);
        d("QUEST_200", CAT_ADV, "Legenda Misi", "Klaim 200 hadiah quest.", R.drawable.ic_target, 4, Trigger.QUEST_CLAIMS, 200);
        d("SHIELD_1", CAT_ADV, "Diselamatkan Perisai", "Streak terselamatkan oleh item pelindung.", R.drawable.ic_shield, 1, Trigger.SHIELD_SAVES, 1);
        d("SHIELD_10", CAT_ADV, "Penjaga Streak", "Streak terselamatkan item pelindung 10 kali.", R.drawable.ic_shield, 3, Trigger.SHIELD_SAVES, 10);

        // ---- Rahasia ----
        d("NIGHT_OWL", CAT_SECRET, "Burung Hantu", "Setor di tengah malam (00.00 - 04.59).", R.drawable.ic_moon, TIER_SECRET, Trigger.EVENT_NIGHT, 0);
        d("EARLY_BIRD", CAT_SECRET, "Si Pagi", "Setor di pagi hari (05.00 - 07.59).", R.drawable.ic_sun, TIER_SECRET, Trigger.EVENT_MORNING, 0);
        d("NEW_YEAR", CAT_SECRET, "Tahun Baru Hemat", "Setor tepat pada 1 Januari.", R.drawable.ic_calendar, TIER_SECRET, Trigger.EVENT_NEW_YEAR, 0);
        d("SURVIVOR", CAT_SECRET, "Nyaris Tewas", "Pulihkan HP dengan item saat HP di bawah 20.", R.drawable.ic_heart, TIER_SECRET, Trigger.EVENT_SURVIVOR, 0);
    }

    public static final List<Def> ALL = Collections.unmodifiableList(LIST);

    public static Def find(String code) {
        for (Def d : ALL) if (d.code.equals(code)) return d;
        return null;
    }

    public static List<BadgeEntity> entities() {
        List<BadgeEntity> l = new ArrayList<>();
        for (Def d : ALL) {
            BadgeEntity b = new BadgeEntity();
            b.code = d.code;
            b.title = d.title;
            b.description = d.description;
            b.category = d.category;
            b.tier = d.tier;
            l.add(b);
        }
        return l;
    }
}
