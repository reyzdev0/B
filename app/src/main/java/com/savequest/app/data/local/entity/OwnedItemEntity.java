package com.savequest.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Item kosmetik yang sudah dibeli di Toko. */
@Entity(tableName = "owned_items")
public class OwnedItemEntity {
    @PrimaryKey @NonNull public String itemId = "";
    public long acquiredAt;
}
