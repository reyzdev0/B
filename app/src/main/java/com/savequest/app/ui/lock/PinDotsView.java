package com.savequest.app.ui.lock;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import com.savequest.app.ui.common.Ui;

/** Indikator PIN: titik neon yang "pop" saat terisi, bergetar saat salah, berdenyut saat benar. */
public class PinDotsView extends View {
    private static final int CYAN = 0xFF00E5FF, RED = 0xFFFF4D6D, GREEN = 0xFF4CFF8F;

    private final int total = 6;
    private int filled = 0;
    private boolean success, error;
    private final float[] scale = new float[total];
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float r, gap, pad;

    public PinDotsView(Context c) { this(c, null); }

    public PinDotsView(Context c, AttributeSet a) {
        super(c, a);
        r = Ui.dp(c, 7);
        gap = Ui.dp(c, 18);
        pad = Ui.dp(c, 14);
        setLayerType(LAYER_TYPE_SOFTWARE, null); // BlurMaskFilter butuh software layer
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(Ui.dp(c, 1.5f));
        ring.setColor(0x66FFFFFF);
        glow.setMaskFilter(new BlurMaskFilter(Ui.dp(c, 8), BlurMaskFilter.Blur.NORMAL));
        for (int i = 0; i < total; i++) scale[i] = 1f;
    }

    public void setFilled(int n) {
        int old = filled;
        filled = Math.max(0, Math.min(total, n));
        if (filled == 0) {
            success = false;
            error = false;
        }
        if (filled > old) pop(filled - 1);
        invalidate();
    }

    private void pop(int idx) {
        ValueAnimator a = ValueAnimator.ofFloat(0.5f, 1f);
        a.setDuration(260);
        a.setInterpolator(new OvershootInterpolator(3f));
        a.addUpdateListener(x -> {
            scale[idx] = (float) x.getAnimatedValue();
            invalidate();
        });
        a.start();
    }

    public void showError() {
        error = true;
        invalidate();
        ObjectAnimator.ofFloat(this, "translationX", 0, -26, 26, -20, 20, -12, 12, 0)
                .setDuration(420).start();
    }

    public void showSuccess() {
        success = true;
        invalidate();
        animate().scaleX(1.18f).scaleY(1.18f).setDuration(160)
                .withEndAction(() -> animate().scaleX(1f).scaleY(1f).setDuration(160).start())
                .start();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = (int) (total * 2 * r + (total - 1) * gap + 2 * pad);
        int h = (int) (2 * r + 2 * pad);
        setMeasuredDimension(resolveSize(w, wSpec), resolveSize(h, hSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float totalW = total * 2 * r + (total - 1) * gap;
        float startX = (getWidth() - totalW) / 2f + r;
        float cy = getHeight() / 2f;
        int color = error ? RED : success ? GREEN : CYAN;
        for (int i = 0; i < total; i++) {
            float cx = startX + i * (2 * r + gap);
            if (i < filled) {
                glow.setColor(color);
                fill.setColor(color);
                canvas.drawCircle(cx, cy, r * scale[i] * 1.25f, glow);
                canvas.drawCircle(cx, cy, r * scale[i], fill);
            } else {
                canvas.drawCircle(cx, cy, r, ring);
            }
        }
    }
}
