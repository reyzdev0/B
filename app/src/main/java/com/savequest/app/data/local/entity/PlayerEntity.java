package com.savequest.app.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Satu baris saja (id = 1): profil RPG milik user. */
@Entity(tableName = "player")
public class PlayerEntity {
    @PrimaryKey public int id = 1;
    public long totalExp;
    public int streak;
    public int longestStreak;
    /** Epoch day terakhir user menabung, -1 jika belum pernah. */
    public long lastDepositDay = -1;

    // ---- v2.1 ----
    @ColumnInfo(defaultValue = "0") public long coins;
    @ColumnInfo(defaultValue = "100") public int hp = 100;
    public String avatarId = "hero";
    public String frameId = "frame_none";
    public String skinId = "skin_default";
    public String bgId = "bg_default";
    /** Path file foto avatar dari galeri (internal storage). */
    public String customAvatarPath;
    /** Terakhir kali HP dicek untuk hari yang terlewat. */
    @ColumnInfo(defaultValue = "-1") public long lastHpCheckDay = -1;
    @ColumnInfo(defaultValue = "-1") public long lastSpinDay = -1;
    @ColumnInfo(defaultValue = "1.0") public double buffMultiplier = 1.0;
    /** Buff EXP berlaku sampai epoch day ini (inklusif). */
    @ColumnInfo(defaultValue = "-1") public long buffUntilDay = -1;

    // ---- v2.2: booster EXP (dari item) ----
    @ColumnInfo(defaultValue = "1.0") public double boostMult = 1.0;
    /** Sisa jumlah setoran yang diberi booster (item "Ramuan EXP"). */
    @ColumnInfo(defaultValue = "0") public int boostCharges;
    /** Booster berbasis waktu aktif sampai epoch millis ini (item "Elixir EXP"). */
    @ColumnInfo(defaultValue = "0") public long boostUntil;

    // ---- v2.2: penghitung untuk badge & statistik ----
    @ColumnInfo(defaultValue = "0") public long depositCount;
    @ColumnInfo(defaultValue = "0") public long peakTotal;
    @ColumnInfo(defaultValue = "0") public long maxDeposit;
    @ColumnInfo(defaultValue = "0") public long lifetimeCoins;
    @ColumnInfo(defaultValue = "0") public long itemsUsed;
    @ColumnInfo(defaultValue = "0") public long purchaseCount;
    @ColumnInfo(defaultValue = "0") public long spinCount;
    @ColumnInfo(defaultValue = "0") public long questClaims;
    @ColumnInfo(defaultValue = "0") public long shieldSaves;
    @ColumnInfo(defaultValue = "0") public long bossKills;
    @ColumnInfo(defaultValue = "0") public long vaultsDone;
}
