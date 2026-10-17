package com.savequest.app.domain;

import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Item kosmetik Toko: bingkai profil, skin (warna karakter), dan latar belakang. Harga dalam koin. */
public final class CosmeticCatalog {
    private CosmeticCatalog() {}

    public static final String FRAME = "FRAME", SKIN = "SKIN", BG = "BG";
    public static final String FRAME_NONE = "frame_none", SKIN_DEFAULT = "skin_default", BG_DEFAULT = "bg_default";

    public static final class Item {
        public final String id, category, name;
        public final int price;
        /** Warna gradient (bingkai / latar). */
        public final int c1, c2, c3;
        /** Khusus skin: rotasi hue (derajat), saturasi, kecerahan. */
        public final float hue, sat, bright;

        Item(String id, String category, String name, int price, int c1, int c2, int c3,
             float hue, float sat, float bright) {
            this.id = id;
            this.category = category;
            this.name = name;
            this.price = price;
            this.c1 = c1;
            this.c2 = c2;
            this.c3 = c3;
            this.hue = hue;
            this.sat = sat;
            this.bright = bright;
        }

        public boolean isFree() { return price == 0; }
    }

    private static Item ring(String id, String name, int price, int a, int b, int c) {
        return new Item(id, FRAME, name, price, a, b, c, 0, 1, 1);
    }

    private static Item bg(String id, String name, int price, int a, int b) {
        return new Item(id, BG, name, price, a, b, b, 0, 1, 1);
    }

    private static Item skin(String id, String name, int price, float hue, float sat, float bright) {
        return new Item(id, SKIN, name, price, 0, 0, 0, hue, sat, bright);
    }

    public static final List<Item> ALL = Arrays.asList(
            ring(FRAME_NONE, "Bingkai Level", 0, 0, 0, 0),
            ring("frame_bronze", "Perunggu", 60, 0xFFE6A15C, 0xFFB8692B, 0xFFE6A15C),
            ring("frame_silver", "Perak", 120, 0xFFCFD8DC, 0xFF78909C, 0xFFCFD8DC),
            ring("frame_gold", "Emas", 250, 0xFFFFCA28, 0xFFFB8C00, 0xFFFFCA28),
            ring("frame_neon", "Neon", 400, 0xFF00E5FF, 0xFF8E7CFF, 0xFF00E5FF),
            ring("frame_flame", "Api", 500, 0xFFFF7A1A, 0xFFFFEB3B, 0xFFE53935),
            ring("frame_royal", "Kerajaan", 800, 0xFF8E7CFF, 0xFFFFCA28, 0xFF5B3FD6),

            skin(SKIN_DEFAULT, "Asli", 0, 0, 1f, 1f),
            skin("skin_ocean", "Samudra", 150, 170, 1.1f, 1f),
            skin("skin_forest", "Hutan", 150, 90, 1.1f, 1f),
            skin("skin_rose", "Mawar", 200, -60, 1.2f, 1.05f),
            skin("skin_gold", "Emas", 400, 35, 1.8f, 1.12f),
            skin("skin_shadow", "Bayangan", 300, 0, 0.25f, 0.7f),

            bg(BG_DEFAULT, "Polos", 0, 0xFFB0BEC5, 0xFF78909C),
            bg("bg_dawn", "Fajar", 80, 0xFFFFB74D, 0xFFFF7043),
            bg("bg_ocean", "Lautan", 100, 0xFF4FC3F7, 0xFF0277BD),
            bg("bg_forest", "Rimba", 100, 0xFF81C784, 0xFF2E7D32),
            bg("bg_sakura", "Sakura", 150, 0xFFF8BBD0, 0xFFEC407A),
            bg("bg_midnight", "Tengah Malam", 200, 0xFF3949AB, 0xFF0D1033),
            bg("bg_galaxy", "Galaksi", 350, 0xFF7E57C2, 0xFF0D0633));

    public static Item find(String id) {
        if (id == null) return null;
        for (Item i : ALL) if (i.id.equals(id)) return i;
        return null;
    }

    public static List<Item> list(String category) {
        List<Item> out = new ArrayList<>();
        for (Item i : ALL) if (i.category.equals(category)) out.add(i);
        return out;
    }

    /** Filter warna untuk skin (null = tanpa perubahan). */
    public static ColorFilter skinFilter(String skinId) {
        Item it = find(skinId);
        if (it == null || (it.hue == 0 && it.sat == 1f && it.bright == 1f)) return null;
        ColorMatrix m = new ColorMatrix();
        m.setSaturation(it.sat);
        double rad = Math.toRadians(it.hue);
        float cos = (float) Math.cos(rad), sin = (float) Math.sin(rad);
        ColorMatrix h = new ColorMatrix(new float[]{
                0.213f + cos * 0.787f - sin * 0.213f, 0.715f - cos * 0.715f - sin * 0.715f, 0.072f - cos * 0.072f + sin * 0.928f, 0, 0,
                0.213f - cos * 0.213f + sin * 0.143f, 0.715f + cos * 0.285f + sin * 0.140f, 0.072f - cos * 0.072f - sin * 0.283f, 0, 0,
                0.213f - cos * 0.213f - sin * 0.787f, 0.715f - cos * 0.715f + sin * 0.715f, 0.072f + cos * 0.928f + sin * 0.072f, 0, 0,
                0, 0, 0, 1, 0});
        m.postConcat(h);
        ColorMatrix b = new ColorMatrix();
        b.setScale(it.bright, it.bright, it.bright, 1f);
        m.postConcat(b);
        return new ColorMatrixColorFilter(m);
    }
}
