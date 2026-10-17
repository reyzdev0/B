package com.savequest.app.ui.nav;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.savequest.app.R;
import com.savequest.app.ui.common.Ui;

/**
 * Bottom navigation kapsul melayang: [Badge] [Home] [Pengaturan].
 * Lingkaran hijau (indikator) meluncur ke menu aktif; ikon + label tampil di dalam lingkaran,
 * menu tidak aktif hanya menampilkan ikon.
 */
public class FloatingNavBar extends FrameLayout {
    public interface Listener {
        void onSelected(int index);
    }

    private static final int[] ICONS = {R.drawable.ic_trophy, R.drawable.ic_home, R.drawable.ic_settings};
    private static final String[] LABELS = {"Badge", "Home", "Pengaturan"};

    private final View indicator;
    private final ImageView[] icons = new ImageView[3];
    private final TextView[] labels = new TextView[3];
    private final int indicatorSize, hMargin, lift, inactiveColor;
    private int selected = 1;
    private boolean sized;
    private Listener listener;

    public FloatingNavBar(Context c) { this(c, null); }

    public FloatingNavBar(Context c, AttributeSet attrs) {
        super(c, attrs);
        setClipChildren(false);
        setClipToPadding(false);

        int capsuleH = Ui.dp(c, 64);
        indicatorSize = Ui.dp(c, 76);
        hMargin = Ui.dp(c, 16);
        lift = Ui.dp(c, 9);
        inactiveColor = Ui.color(c, R.color.sq_icon_inactive);
        int green1 = Ui.color(c, R.color.sq_green_1);
        int green2 = Ui.color(c, R.color.sq_green_2);

        // kapsul latar dengan drop shadow halus
        View capsule = new View(c);
        capsule.setBackground(Ui.rounded(Ui.color(c, R.color.sq_surface_alt), capsuleH / 2f));
        capsule.setElevation(Ui.dp(c, 8));
        LayoutParams cp = new LayoutParams(LayoutParams.MATCH_PARENT, capsuleH, Gravity.CENTER_VERTICAL);
        cp.setMargins(hMargin, 0, hMargin, 0);
        addView(capsule, cp);

        // lingkaran indikator (bergeser)
        indicator = new View(c);
        indicator.setBackground(Ui.oval(green1, green2));
        indicator.setElevation(Ui.dp(c, 12));
        if (Build.VERSION.SDK_INT >= 28) {
            indicator.setOutlineAmbientShadowColor(green1);
            indicator.setOutlineSpotShadowColor(green1);
        }
        addView(indicator, new LayoutParams(indicatorSize, indicatorSize, Gravity.CENTER_VERTICAL | Gravity.START));

        // baris ikon + label (di atas indikator)
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setElevation(Ui.dp(c, 14));
        LayoutParams rp = new LayoutParams(LayoutParams.MATCH_PARENT, indicatorSize, Gravity.CENTER_VERTICAL);
        rp.setMargins(hMargin, 0, hMargin, 0);
        addView(row, rp);

        for (int i = 0; i < 3; i++) {
            FrameLayout slot = new FrameLayout(c);
            slot.setContentDescription(LABELS[i]);
            slot.setClickable(true);
            slot.setFocusable(true);

            ImageView iv = new ImageView(c);
            iv.setImageResource(ICONS[i]);
            slot.addView(iv, new FrameLayout.LayoutParams(Ui.dp(c, 24), Ui.dp(c, 24), Gravity.CENTER));
            icons[i] = iv;

            TextView tv = new TextView(c);
            tv.setText(LABELS[i]);
            tv.setTextColor(Color.WHITE);
            tv.setTextSize(11);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setAlpha(0f);
            tv.setTranslationY(Ui.dp(c, 22));
            slot.addView(tv, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
            labels[i] = tv;

            final int idx = i;
            slot.setOnClickListener(v -> onTap(idx));
            row.addView(slot, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        }
        applyStates(false);
    }

    public void setListener(Listener l) { listener = l; }

    public int getSelected() { return selected; }

    /** Pilih menu tanpa memanggil listener. */
    public void select(int index, boolean animate) {
        selected = index;
        if (sized) moveIndicator(animate);
        applyStates(animate);
    }

    private void onTap(int index) {
        if (index == selected) return;
        select(index, true);
        if (listener != null) listener.onSelected(index);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        sized = w > 0;
        if (sized) indicator.setTranslationX(targetX(selected));
    }

    private float targetX(int index) {
        float slotW = (getWidth() - 2f * hMargin) / 3f;
        return hMargin + slotW * (index + 0.5f) - indicatorSize / 2f;
    }

    private void moveIndicator(boolean animate) {
        float x = targetX(selected);
        indicator.animate().cancel();
        if (!animate) {
            indicator.setTranslationX(x);
            return;
        }
        indicator.setScaleX(0.8f); // efek "cair": menyempit lalu melebar saat meluncur
        indicator.animate().translationX(x).scaleX(1f).scaleY(1f)
                .setDuration(420)
                .setInterpolator(new OvershootInterpolator(0.8f))
                .start();
    }

    private void applyStates(boolean animate) {
        Context c = getContext();
        for (int i = 0; i < 3; i++) {
            boolean active = i == selected;
            icons[i].setColorFilter(active ? Color.WHITE : inactiveColor);
            float iconY = active ? -lift : 0f;
            float labelY = active ? Ui.dp(c, 15) : Ui.dp(c, 22);
            float labelA = active ? 1f : 0f;
            if (animate) {
                icons[i].animate().translationY(iconY).setDuration(250).start();
                labels[i].animate().alpha(labelA).translationY(labelY).setDuration(250).start();
            } else {
                icons[i].setTranslationY(iconY);
                labels[i].setAlpha(labelA);
                labels[i].setTranslationY(labelY);
            }
        }
    }
}
