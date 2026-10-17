package com.savequest.app.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "vaults")
public class VaultEntity {
    public static final String TYPE_NORMAL = "NORMAL";
    public static final String TYPE_BOSS = "BOSS";
    public static final String TYPE_EMERGENCY = "EMERGENCY";

    @PrimaryKey(autoGenerate = true) public long id;
    public String name;
    public long targetAmount;
    public long savedAmount;
    /** Epoch millis. 0 = tidak dikunci. */
    public long lockUntil;
    public long createdAt;

    // ---- v2.1 ----
    public String type = TYPE_NORMAL;
    /** Bentuk boss (0 = Slime, 1 = Golem, 2 = Naga). Hanya untuk TYPE_BOSS. */
    @ColumnInfo(defaultValue = "0") public int bossKind;
    /** Waktu target pertama kali tercapai (boss kalah). 0 = belum. */
    @ColumnInfo(defaultValue = "0") public long defeatedAt;
}
