package com.savequest.app.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Satu quest untuk satu periode (harian = 1 hari, mingguan = Senin-Minggu). */
@Entity(tableName = "quests",
        indices = @Index(value = {"code", "periodStart"}, unique = true))
public class QuestEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public String code;
    public String title;
    public long targetAmount;
    public int rewardExp;
    public long periodStart; // epoch day
    public long periodEnd;   // epoch day
    public long progress;
    /** Target tercapai (hadiah siap diklaim). */
    public boolean completed;
    /** Hadiah EXP sudah diklaim user. */
    @ColumnInfo(defaultValue = "0") public boolean claimed;
}
