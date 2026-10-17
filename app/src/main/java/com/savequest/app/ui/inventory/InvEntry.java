package com.savequest.app.ui.inventory;

/** Satu slot di Ransel: item habis-pakai (dari tabel inventory) atau kosmetik yang sudah dimiliki. */
public final class InvEntry {
    public static final String CAT_COSMETIC = "COSMETIC";

    public String id;
    public String name;
    public String description;
    /** HEAL, PROTECT, BOOST, SPECIAL, atau COSMETIC. */
    public String category;
    public int quantity;
    public int rarity;
    public int iconRes;
    public boolean cosmetic;
    public boolean equipped;
}
