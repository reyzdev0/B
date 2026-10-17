package com.savequest.app.domain;

import com.savequest.app.data.local.entity.ShopItemEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Katalog item RPG: pemulihan, pelindung streak, booster EXP, dan item spesial. */
public final class ItemCatalog {
    private ItemCatalog() {}

    public static final String CAT_HEAL = "HEAL", CAT_PROTECT = "PROTECT", CAT_BOOST = "BOOST", CAT_SPECIAL = "SPECIAL";
    public static final String[] CATEGORIES = {CAT_HEAL, CAT_PROTECT, CAT_BOOST, CAT_SPECIAL};

    public static final String EFFECT_HEAL = "HEAL", EFFECT_SHIELD = "SHIELD",
            EFFECT_BOOST_CHARGES = "BOOST_CHARGES", EFFECT_BOOST_TIME = "BOOST_TIME",
            EFFECT_EXP = "EXP", EFFECT_COINS = "COINS_RANDOM", EFFECT_SPIN = "SPIN", EFFECT_CRATE = "CRATE";

    public static final String BANDAGE = "bandage", POTION_HP = "potion_hp", ELIXIR_HP = "elixir_hp",
            STREAK_SHIELD = "streak_shield", PROTECT_SCROLL = "protect_scroll",
            EXP_POTION = "exp_potion", EXP_ELIXIR = "exp_elixir", SPIN_TICKET = "spin_ticket",
            COIN_POUCH = "coin_pouch", KNOWLEDGE_SCROLL = "knowledge_scroll", COSMETIC_CRATE = "cosmetic_crate";

    private static final List<ShopItemEntity> ALL = new ArrayList<>();

    private static void add(String id, String cat, String name, String desc, int price, String effect,
                            double value, double extra, int rarity, String icon) {
        ShopItemEntity e = new ShopItemEntity();
        e.id = id;
        e.category = cat;
        e.name = name;
        e.description = desc;
        e.price = price;
        e.effectType = effect;
        e.effectValue = value;
        e.effectExtra = extra;
        e.rarity = rarity;
        e.iconKey = icon;
        e.sortOrder = ALL.size() + 1;
        e.enabled = true;
        ALL.add(e);
    }

    static {
        add(BANDAGE, CAT_HEAL, "Perban", "Memulihkan 15 HP.", 25, EFFECT_HEAL, 15, 0, 1, "bandage");
        add(POTION_HP, CAT_HEAL, "Ramuan HP", "Memulihkan 40 HP.", 60, EFFECT_HEAL, 40, 0, 2, "potion");
        add(ELIXIR_HP, CAT_HEAL, "Elixir Kehidupan", "Memulihkan HP sampai penuh.", 140, EFFECT_HEAL, 100, 0, 3, "elixir");
        add(STREAK_SHIELD, CAT_PROTECT, "Perisai Streak", "Otomatis menyelamatkan streak jika kamu melewatkan 1 hari menabung.", 80, EFFECT_SHIELD, 1, 0, 2, "shield");
        add(PROTECT_SCROLL, CAT_PROTECT, "Gulungan Pelindung", "Otomatis menyelamatkan streak meski terlewat sampai 3 hari.", 200, EFFECT_SHIELD, 3, 0, 3, "scroll");
        add(EXP_POTION, CAT_BOOST, "Ramuan EXP", "EXP x1.5 untuk 3 setoran berikutnya.", 70, EFFECT_BOOST_CHARGES, 1.5, 3, 2, "potion");
        add(EXP_ELIXIR, CAT_BOOST, "Elixir EXP", "EXP x2 selama 24 jam.", 160, EFFECT_BOOST_TIME, 2.0, 24, 3, "elixir");
        add(KNOWLEDGE_SCROLL, CAT_BOOST, "Gulungan Ilmu", "Langsung mendapat +100 EXP.", 120, EFFECT_EXP, 100, 0, 2, "book");
        add(SPIN_TICKET, CAT_SPECIAL, "Tiket Spin", "Putar roda keberuntungan sekali lagi setelah spin harian.", 90, EFFECT_SPIN, 1, 0, 2, "ticket");
        add(COIN_POUCH, CAT_SPECIAL, "Kantong Koin", "Berisi 30 sampai 90 koin acak.", 60, EFFECT_COINS, 30, 90, 1, "pouch");
        add(COSMETIC_CRATE, CAT_SPECIAL, "Peti Kosmetik", "Berisi 1 bingkai, skin, atau latar acak yang belum kamu punya.", 220, EFFECT_CRATE, 0, 0, 3, "chest");
    }

    public static List<ShopItemEntity> defaults() {
        return Collections.unmodifiableList(ALL);
    }

    public static ShopItemEntity find(String id) {
        for (ShopItemEntity e : ALL) if (e.id.equals(id)) return e;
        return null;
    }

    public static String categoryLabel(String cat) {
        if (CAT_HEAL.equals(cat)) return "Pemulihan";
        if (CAT_PROTECT.equals(cat)) return "Pelindung";
        if (CAT_BOOST.equals(cat)) return "Booster";
        if (CAT_SPECIAL.equals(cat)) return "Spesial";
        return "Kosmetik";
    }

    public static int rarityColor(int rarity) {
        switch (rarity) {
            case 2: return 0xFF42A5F5;
            case 3: return 0xFFAB47BC;
            case 4: return 0xFFFFCA28;
            default: return 0xFF90A4AE;
        }
    }

    public static String rarityName(int rarity) {
        switch (rarity) {
            case 2: return "Langka";
            case 3: return "Epik";
            case 4: return "Legendaris";
            default: return "Biasa";
        }
    }
}
