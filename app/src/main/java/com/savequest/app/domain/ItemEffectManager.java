package com.savequest.app.domain;

import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.data.local.entity.ShopItemEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pusat logika efek item (tanpa akses database). Dipanggil repository di dalam transaksi:
 * mengubah PlayerEntity sesuai efek lalu mengembalikan ringkasan untuk UI.
 *
 *  - HEAL            : HP +value (>= 100 berarti penuh)
 *  - SHIELD          : pasif. Dipakai otomatis saat deposit setelah hari terlewat (lihat pickProtection)
 *  - BOOST_CHARGES   : EXP x value untuk "extra" setoran berikutnya
 *  - BOOST_TIME      : EXP x value selama "extra" jam
 *  - EXP             : EXP +value
 *  - COINS_RANDOM    : koin acak antara value..extra
 *  - SPIN            : membuka spin tambahan hari ini
 *  - CRATE           : 1 kosmetik acak yang belum dimiliki (atau 150 koin jika sudah lengkap)
 */
public final class ItemEffectManager {
    private ItemEffectManager() {}

    public static final class Outcome {
        public String error;
        public String message;
        /** false = item tidak berkurang (mis. HP sudah penuh, atau item pasif). */
        public boolean consume = true;
        public int hpDelta;
        public int exp;
        public long coins;
        public String cosmeticId;
        public boolean healedFromCritical;
    }

    public static boolean boostActive(PlayerEntity p, long nowMillis) {
        return p.boostCharges > 0 || nowMillis < p.boostUntil;
    }

    /** Pengali booster saat ini (1.0 jika tidak ada). */
    public static double boostMultiplier(PlayerEntity p, long nowMillis) {
        return boostActive(p, nowMillis) && p.boostMult > 1.0 ? p.boostMult : 1.0;
    }

    /**
     * Item pelindung yang dipakai saat streak hampir putus.
     * @param gap jumlah hari yang terlewat (1..3)
     * @return id item yang dikonsumsi, atau null jika tidak bisa diselamatkan
     */
    public static String pickProtection(int gap, int shields, int scrolls) {
        if (gap == 1) {
            if (shields > 0) return ItemCatalog.STREAK_SHIELD;
            if (scrolls > 0) return ItemCatalog.PROTECT_SCROLL;
        } else if (gap >= 2 && gap <= 3 && scrolls > 0) {
            return ItemCatalog.PROTECT_SCROLL;
        }
        return null;
    }

    public static Outcome apply(ShopItemEntity item, PlayerEntity p, Random rnd, long nowMillis,
                                long today, Set<String> ownedCosmetics) {
        Outcome o = new Outcome();
        switch (item.effectType) {
            case ItemCatalog.EFFECT_HEAL: {
                if (p.hp >= HpSystem.MAX) {
                    o.error = "HP kamu sudah penuh";
                    o.consume = false;
                    return o;
                }
                o.healedFromCritical = p.hp < 20;
                int before = p.hp;
                p.hp = Math.min(HpSystem.MAX, p.hp + (int) item.effectValue);
                o.hpDelta = p.hp - before;
                o.message = item.name + ": HP +" + o.hpDelta + " (sekarang " + p.hp + "/" + HpSystem.MAX + ")";
                return o;
            }
            case ItemCatalog.EFFECT_SHIELD:
                o.error = item.name + " aktif otomatis saat kamu melewatkan hari menabung";
                o.consume = false;
                return o;
            case ItemCatalog.EFFECT_BOOST_CHARGES:
            case ItemCatalog.EFFECT_BOOST_TIME: {
                if (boostActive(p, nowMillis)) {
                    o.error = "Booster lain masih aktif. Tunggu sampai habis.";
                    o.consume = false;
                    return o;
                }
                p.boostMult = item.effectValue;
                if (ItemCatalog.EFFECT_BOOST_CHARGES.equals(item.effectType)) {
                    p.boostCharges = (int) item.effectExtra;
                    p.boostUntil = 0;
                    o.message = "EXP x" + StreakSystem.label(item.effectValue) + " aktif untuk "
                            + (int) item.effectExtra + " setoran";
                } else {
                    p.boostCharges = 0;
                    p.boostUntil = nowMillis + (long) (item.effectExtra * 3_600_000L);
                    o.message = "EXP x" + StreakSystem.label(item.effectValue) + " aktif selama "
                            + (int) item.effectExtra + " jam";
                }
                return o;
            }
            case ItemCatalog.EFFECT_EXP:
                p.totalExp += (long) item.effectValue;
                o.exp = (int) item.effectValue;
                o.message = item.name + ": +" + o.exp + " EXP";
                return o;
            case ItemCatalog.EFFECT_COINS: {
                int min = (int) item.effectValue, max = (int) item.effectExtra;
                o.coins = min + rnd.nextInt(Math.max(1, max - min + 1));
                p.coins += o.coins;
                p.lifetimeCoins += o.coins;
                o.message = item.name + ": +" + o.coins + " koin";
                return o;
            }
            case ItemCatalog.EFFECT_SPIN:
                if (p.lastSpinDay != today) {
                    o.error = "Spin harianmu masih tersedia. Putar dulu rodanya.";
                    o.consume = false;
                    return o;
                }
                p.lastSpinDay = -1;
                o.message = "Roda keberuntungan siap diputar lagi";
                return o;
            case ItemCatalog.EFFECT_CRATE: {
                List<String> pool = new ArrayList<>();
                for (CosmeticCatalog.Item c : CosmeticCatalog.ALL) {
                    if (!c.isFree() && !ownedCosmetics.contains(c.id)) pool.add(c.id);
                }
                if (pool.isEmpty()) {
                    o.coins = 150;
                    p.coins += 150;
                    p.lifetimeCoins += 150;
                    o.message = "Semua kosmetik sudah kamu miliki. Peti diganti 150 koin";
                } else {
                    o.cosmeticId = pool.get(rnd.nextInt(pool.size()));
                    CosmeticCatalog.Item got = CosmeticCatalog.find(o.cosmeticId);
                    o.message = "Kamu mendapat kosmetik: " + (got == null ? o.cosmeticId : got.name);
                }
                return o;
            }
            default:
                o.error = "Item ini belum bisa digunakan";
                o.consume = false;
                return o;
        }
    }
}
