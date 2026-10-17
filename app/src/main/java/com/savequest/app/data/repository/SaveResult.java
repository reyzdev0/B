package com.savequest.app.data.repository;

import com.savequest.app.data.local.entity.QuestEntity;
import com.savequest.app.domain.BadgeCatalog;
import com.savequest.app.domain.StreakSystem;

import java.util.ArrayList;
import java.util.List;

/** Hasil satu operasi. UI memakainya untuk haptic, toast, dan dialog. */
public class SaveResult {
    public static final String DEPOSIT = "DEPOSIT";
    public static final String WITHDRAW = "WITHDRAW";
    public static final String CLAIM = "CLAIM";
    public static final String SPIN = "SPIN";
    public static final String HP = "HP";
    public static final String SHOP = "SHOP";
    public static final String ITEM = "ITEM";
    public static final String BADGE = "BADGE";
    public static final String OTHER = "OTHER";

    public String kind = OTHER;
    public String error;
    public String info;
    public int baseExp;
    public int expGained;
    public int expLost;
    public long coinsGained;
    /** Perubahan HP (negatif = berkurang). */
    public int hpDelta;
    public double multiplier = 1.0;
    public double buff = 1.0;
    public boolean leveledUp;
    public int newLevel;
    /** Indeks hadiah roda (kind SPIN). */
    public int spinIndex = -1;
    /** Id item pelindung yang otomatis terpakai untuk menyelamatkan streak. */
    public String shieldUsed;
    /** Teks item yang didapat, mis. "Perban x1". */
    public final List<String> itemsGranted = new ArrayList<>();
    public final List<QuestEntity> questsReady = new ArrayList<>();
    public final List<BadgeCatalog.Def> badges = new ArrayList<>();
    public final List<String> bossesDefeated = new ArrayList<>();

    public String summary() {
        if (error != null) return error;
        if (info != null) return info;
        StringBuilder s = new StringBuilder("+" + expGained + " EXP");
        if (coinsGained > 0) s.append(", +").append(coinsGained).append(" koin");
        if (multiplier > 1.0) s.append(" (streak ").append(StreakSystem.label(multiplier)).append(")");
        if (buff > 1.0) s.append(" (booster ").append(StreakSystem.label(buff)).append(")");
        if (shieldUsed != null) s.append("\nStreak diselamatkan oleh item pelindung");
        return s.toString();
    }
}
