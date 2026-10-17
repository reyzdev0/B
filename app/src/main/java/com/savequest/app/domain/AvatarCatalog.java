package com.savequest.app.domain;

import com.savequest.app.R;

import java.util.Arrays;
import java.util.List;

/**
 * Koleksi avatar preset. Hero berevolusi 5 tahap mengikuti level (1, 3, 5, 10, 20).
 * Avatar lain terbuka pada level tertentu. "custom" = foto dari galeri.
 */
public final class AvatarCatalog {
    private AvatarCatalog() {}

    public static final String HERO = "hero", FOX = "fox", ROBOT = "robot",
            WIZARD = "wizard", DRAGON = "dragon", CUSTOM = "custom";

    public static final int[] STAGE_LEVELS = {1, 3, 5, 10, 20};
    public static final String[] STAGE_NAMES = {"Pemula", "Penabung Rajin", "Veteran", "Master", "Legenda"};

    public static final class Def {
        public final String id, name;
        public final int minLevel;
        public final int[] drawables;

        Def(String id, String name, int minLevel, int... drawables) {
            this.id = id;
            this.name = name;
            this.minLevel = minLevel;
            this.drawables = drawables;
        }
    }

    public static final List<Def> ALL = Arrays.asList(
            new Def(HERO, "Hero", 1, R.drawable.av_hero_1, R.drawable.av_hero_2,
                    R.drawable.av_hero_3, R.drawable.av_hero_4, R.drawable.av_hero_5),
            new Def(FOX, "Rubah", 3, R.drawable.av_fox),
            new Def(ROBOT, "Robot", 5, R.drawable.av_robot),
            new Def(WIZARD, "Penyihir", 8, R.drawable.av_wizard),
            new Def(DRAGON, "Naga", 12, R.drawable.av_dragon));

    /** Tahap evolusi 0..4 berdasarkan level. */
    public static int stage(int level) {
        if (level >= 20) return 4;
        if (level >= 10) return 3;
        if (level >= 5) return 2;
        if (level >= 3) return 1;
        return 0;
    }

    public static Def find(String id) {
        for (Def d : ALL) if (d.id.equals(id)) return d;
        return null;
    }

    public static int drawableFor(String id, int level) {
        Def d = find(id == null ? HERO : id);
        if (d == null) d = ALL.get(0);
        return d.drawables.length > 1 ? d.drawables[stage(level)] : d.drawables[0];
    }
}
