package com.savequest.app.ui.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.savequest.app.ui.common.Ui;

import java.util.Random;

/**
 * Roda keberuntungan (Canvas). Segmen digambar dengan drawArc, label diputar bersama roda.
 * spinTo(index) memutar beberapa putaran penuh lalu berhenti tepat di segmen hadiah.
 */
public class SpinWheelView extends View {
    public interface Listener {
        void onFinished(int index);
    }

    private static final int[] COLORS = {0xFF66BB6A, 0xFFFFCA28, 0xFF42A5F5, 0xFFEF5350,
            0xFFAB47BC, 0xFFFF7043, 0xFF26C6DA, 0xFFEC407A};

    private String[] labels = new String[0];
    private float angle = 0f;
    private boolean spinning = false;
    private final Random rnd = new Random();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();

    public SpinWheelView(Context c) { this(c, null); }

    public SpinWheelView(Context c, AttributeSet a) {
        super(c, a);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setColor(0xFFFFFFFF);
        stroke.setStrokeWidth(Ui.dp(c, 2));
        text.setColor(0xFFFFFFFF);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setShadowLayer(3f, 0, 1, 0x66000000);
    }

    public void setLabels(String[] l) {
        labels = l;
        invalidate();
    }

    public boolean isSpinning() { return spinning; }

    public void spinTo(final int index, final Listener l) {
        if (labels.length == 0 || spinning) return;
        spinning = true;
        float seg = 360f / labels.length;
        float jitter = (rnd.nextFloat() - 0.5f) * seg * 0.6f;
        float base = angle - (angle % 360f);
        float target = base + 360f * 6 + (360f - (index + 0.5f) * seg) + jitter;
        ValueAnimator anim = ValueAnimator.ofFloat(angle, target);
        anim.setDuration(4200);
        anim.setInterpolator(new DecelerateInterpolator(2.2f));
        anim.addUpdateListener(a -> {
            angle = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                spinning = false;
                if (l != null) l.onFinished(index);
            }
        });
        anim.start();
    }

    @Override
    protected void onMeasure(int w, int h) {
        int size = Math.min(MeasureSpec.getSize(w), Ui.dp(getContext(), 320));
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas c) {
        int n = labels.length;
        if (n == 0) return;
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float r = Math.min(cx, cy) - Ui.dp(getContext(), 18);
        float seg = 360f / n;
        oval.set(cx - r, cy - r, cx + r, cy + r);
        text.setTextSize(r * 0.13f);

        for (int i = 0; i < n; i++) {
            fill.setColor(COLORS[i % COLORS.length]);
            c.drawArc(oval, angle + i * seg - 90f, seg, true, fill);
            c.drawArc(oval, angle + i * seg - 90f, seg, true, stroke);
            c.save();
            c.rotate(angle + (i + 0.5f) * seg, cx, cy);
            c.drawText(labels[i], cx, cy - r * 0.62f, text);
            c.restore();
        }

        // pusat roda
        fill.setColor(0xFFFFFFFF);
        c.drawCircle(cx, cy, r * 0.14f, fill);
        fill.setColor(0xFF2E9D4F);
        c.drawCircle(cx, cy, r * 0.08f, fill);

        // penunjuk di atas
        Path tri = new Path();
        float pw = Ui.dp(getContext(), 13);
        tri.moveTo(cx - pw, cy - r - Ui.dp(getContext(), 14));
        tri.lineTo(cx + pw, cy - r - Ui.dp(getContext(), 14));
        tri.lineTo(cx, cy - r + Ui.dp(getContext(), 12));
        tri.close();
        fill.setColor(0xFFE53935);
        c.drawPath(tri, fill);
    }
}
