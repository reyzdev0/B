package com.savequest.app.domain;

import com.savequest.app.data.local.entity.PlayerEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

/** Mengevaluasi syarat (trigger) semua badge terhadap data pemain. Tanpa akses database. */
public final class BadgeEngine {
    private BadgeEngine() {}

    public static final String EVENT_DEPOSIT = "DEPOSIT";
    public static final String EVENT_HEAL_CRITICAL = "HEAL_CRITICAL";

    /** Nilai ukur saat ini untuk sebuah trigger (dipakai juga untuk indikator progres di UI). */
    public static long metric(BadgeCatalog.Trigger t, PlayerEntity p) {
        switch (t) {
            case DEPOSITS: return p.depositCount;
            case STREAK: return Math.max(p.longestStreak, p.streak);
            case PEAK_TOTAL: return p.peakTotal;
            case LEVEL: return LevelSystem.levelFor(p.totalExp);
            case LIFETIME_COINS: return p.lifetimeCoins;
            case VAULTS_DONE: return p.vaultsDone;
            case BOSS_KILLS: return p.bossKills;
            case ITEMS_USED: return p.itemsUsed;
            case PURCHASES: return p.purchaseCount;
            case SPINS: return p.spinCount;
            case QUEST_CLAIMS: return p.questClaims;
            case SHIELD_SAVES: return p.shieldSaves;
            case MAX_DEPOSIT: return p.maxDeposit;
            default: return 0;
        }
    }

    public static boolean satisfied(BadgeCatalog.Def d, PlayerEntity p, Calendar now, Set<String> events) {
        boolean deposit = events.contains(EVENT_DEPOSIT);
        switch (d.trigger) {
            case EVENT_NIGHT:
                return deposit && now.get(Calendar.HOUR_OF_DAY) < 5;
            case EVENT_MORNING: {
                int h = now.get(Calendar.HOUR_OF_DAY);
                return deposit && h >= 5 && h < 8;
            }
            case EVENT_NEW_YEAR:
                return deposit && now.get(Calendar.MONTH) == Calendar.JANUARY
                        && now.get(Calendar.DAY_OF_MONTH) == 1;
            case EVENT_SURVIVOR:
                return events.contains(EVENT_HEAL_CRITICAL);
            default:
                return metric(d.trigger, p) >= d.threshold;
        }
    }

    /** Badge yang baru terpenuhi syaratnya dan belum pernah dibuka. */
    public static List<BadgeCatalog.Def> newlyUnlocked(PlayerEntity p, Set<String> alreadyUnlocked,
                                                       Calendar now, Set<String> events) {
        List<BadgeCatalog.Def> out = new ArrayList<>();
        for (BadgeCatalog.Def d : BadgeCatalog.ALL) {
            if (alreadyUnlocked.contains(d.code)) continue;
            if (satisfied(d, p, now, events)) out.add(d);
        }
        return out;
    }
}
