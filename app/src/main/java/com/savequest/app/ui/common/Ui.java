package com.savequest.app.ui.common;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.savequest.app.R;

/** Helper kecil untuk ukuran, warna, dan drawable yang dibuat lewat kode. */
public final class Ui {
    private Ui() {}

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static int color(Context c, int res) {
        return ContextCompat.getColor(c, res);
    }

    public static GradientDrawable rounded(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    public static GradientDrawable gradient(int c1, int c2, float radiusPx) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{c1, c2});
        g.setCornerRadius(radiusPx);
        return g;
    }

    public static GradientDrawable oval(int c1, int c2) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{c1, c2});
        g.setShape(GradientDrawable.OVAL);
        return g;
    }

    /** Tombol gradient dengan efek ripple. */
    public static Drawable rippleGradient(int c1, int c2, float radiusPx) {
        return new RippleDrawable(ColorStateList.valueOf(0x44FFFFFF),
                gradient(c1, c2, radiusPx), rounded(Color.WHITE, radiusPx));
    }

    /** Pasangan warna gradient dialog: 0 hijau, 1 merah, 2 emas, 3 ungu. */
    public static int[] accent(Context c, int accent) {
        switch (accent) {
            case 1: return new int[]{color(c, R.color.sq_red_1), color(c, R.color.sq_red_2)};
            case 2: return new int[]{color(c, R.color.sq_gold_1), color(c, R.color.sq_gold_2)};
            case 3: return new int[]{color(c, R.color.sq_purple_1), color(c, R.color.sq_purple_2)};
            default: return new int[]{color(c, R.color.sq_green_1), color(c, R.color.sq_green_2)};
        }
    }

    /** Warna tier badge: 1 Bronze, 2 Silver, 3 Gold, 4 Platinum, 5 Diamond, 6 Rahasia (ungu). */
    public static int[] tier(int tier) {
        switch (tier) {
            case 2: return new int[]{0xFFCFD8DC, 0xFF78909C};
            case 3: return new int[]{0xFFFFCA28, 0xFFFB8C00};
            case 4: return new int[]{0xFFE3F2FD, 0xFF7E9BB8};
            case 5: return new int[]{0xFF80F0FF, 0xFF1E88E5};
            case 6: return new int[]{0xFF8E7CFF, 0xFF5B3FD6};
            default: return new int[]{0xFFE6A15C, 0xFFB8692B};
        }
    }

    public static void styleChip(TextView chip, boolean selected) {
        Context c = chip.getContext();
        if (selected) {
            chip.setBackground(rippleGradient(color(c, R.color.sq_green_1), color(c, R.color.sq_green_2), dp(c, 20)));
            chip.setTextColor(Color.WHITE);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip);
            chip.setTextColor(color(c, R.color.sq_text));
        }
    }
}
