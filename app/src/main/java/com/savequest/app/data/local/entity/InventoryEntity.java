package com.savequest.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Isi ransel: item habis-pakai yang bisa ditumpuk (quantity). */
@Entity(tableName = "inventory")
public class InventoryEntity {
    @PrimaryKey @NonNull public String itemId = "";
    public int quantity;
    public long acquiredAt;
}
