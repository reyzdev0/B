package com.savequest.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Definisi item di Toko. Di-seed dari ItemCatalog tiap aplikasi dibuka, sehingga item baru
 * dari pembaruan aplikasi otomatis muncul tanpa menghapus data.
 */
@Entity(tableName = "shop_items")
public class ShopItemEntity {
    @PrimaryKey @NonNull public String id = "";
    /** HEAL, PROTECT, BOOST, atau SPECIAL. */
    public String category;
    public String name;
    public String description;
    public int price;
    /** Jenis efek, lihat ItemCatalog.EFFECT_*. */
    public String effectType;
    public double effectValue;
    public double effectExtra;
    /** 1 biasa, 2 langka, 3 epik, 4 legendaris. */
    public int rarity;
    public String iconKey;
    public int sortOrder;
    public boolean enabled = true;
}
