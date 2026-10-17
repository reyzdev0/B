package com.savequest.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Status badge. Teks/ikon ditentukan oleh BadgeCatalog (berdasarkan code). */
@Entity(tableName = "badges")
public class BadgeEntity {
    @PrimaryKey @NonNull public String code = "";
    public String title;
    public String description;
    public String category;
    /** 1 Bronze, 2 Silver, 3 Gold, 4 Platinum, 5 Diamond, 6 Rahasia. */
    @ColumnInfo(defaultValue = "1") public int tier = 1;
    /** Epoch millis, 0 = masih terkunci. */
    public long unlockedAt;
}
