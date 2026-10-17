package com.savequest.app.ui.common;

import com.savequest.app.R;

/** Memetakan iconKey item ke vector drawable. */
public final class ItemIcons {
    private ItemIcons() {}

    public static int res(String key) {
        if (key == null) return R.drawable.ic_pouch;
        switch (key) {
            case "bandage": return R.drawable.ic_bandage;
            case "potion": return R.drawable.ic_potion;
            case "elixir": return R.drawable.ic_elixir;
            case "shield": return R.drawable.ic_shield;
            case "scroll": return R.drawable.ic_scroll;
            case "ticket": return R.drawable.ic_ticket;
            case "pouch": return R.drawable.ic_pouch;
            case "book": return R.drawable.ic_book;
            case "chest": return R.drawable.ic_chest;
            default: return R.drawable.ic_pouch;
        }
    }
}
