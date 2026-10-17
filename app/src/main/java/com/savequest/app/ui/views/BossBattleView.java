package com.savequest.app.ui.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import com.savequest.app.domain.BossSystem;
import com.savequest.app.ui.common.Ui;
import com.savequest.app.util.Money;

/**
 * Boss Battle (Canvas): target tabungan jadi boss RPG. Bar HP boss = sisa target.
 * Tiap setoran memicu animasi serangan: tebasan, boss berkedip merah + bergetar, angka damage melayang.
 */
public class BossBattleView extends View {
    private int kind;
    private String title = "";
    private long saved, target, lastSaved = -1;
    private boolean defeated;
    private float hp = 1f;
    private float bob, flash, shake, slash, dmgT = 1f;
    private String dmgText = "";
    private ValueAnimator idle, hit, hpAnim, dmgAnim;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint t = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final float d;

    public BossBattleView(Context c) { this(c, null); }

    public BossBattleView(Context c, AttributeSet a) {
        super(c, a);
        d = c.getResources().getDisplayMetrics().density;
        t.setTypeface(Typeface.DEFAULT_BOLD);
    }

    /** Perbarui status boss. Jika saldo naik dibanding sebelumnya, animasi serangan diputar. */
    public void setBoss(int kind, String title, long saved, long target, boolean defeated) {
        long prev = lastSaved;
        this.kind = kind;
        this.title = title;
        this.saved = saved;
        this.target = target;
        this.defeated = defeated;
        float newHp = BossSystem.hpFraction(saved, target);
        if (prev < 0) {
            hp = newHp;
        } else {
            if (hpAnim != null) hpAnim.cancel();
            hpAnim = ValueAnimator.ofFloat(hp, newHp);
            hpAnim.setDuration(700);
            hpAnim.addUpdateListener(a -> {
                hp = (float) a.getAnimatedValue();
                invalidate();
            });
            hpAnim.start();
            if (saved > prev) playHit(saved - prev);
        }
        lastSaved = saved;
        invalidate();
    }

    private void playHit(long damage) {
        dmgText = "-" + Money.rp(damage);
        if (hit != null) hit.cancel();
        hit = ValueAnimator.ofFloat(0f, 1f);
        hit.setDuration(520);
        hit.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            flash = 1f - f;
            shake = (float) Math.sin(f * Math.PI * 7) * (1f - f) * 7 * d;
            slash = f;
            invalidate();
        });
        hit.start();
        if (dmgAnim != null) dmgAnim.cancel();
        dmgAnim = ValueAnimator.ofFloat(0f, 1f);
        dmgAnim.setDuration(1100);
        dmgAnim.addUpdateListener(a -> {
            dmgT = (float) a.getAnimatedValue();
            invalidate();
        });
        dmgAnim.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        idle = ValueAnimator.ofFloat(0f, (float) (2 * Math.PI));
        idle.setDuration(2400);
        idle.setRepeatCount(ValueAnimator.INFINITE);
        idle.setInterpolator(new LinearInterpolator());
        idle.addUpdateListener(a -> {
            bob = (float) Math.sin((float) a.getAnimatedValue()) * 4 * d;
            invalidate();
        });
        idle.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        for (ValueAnimator a : new ValueAnimator[]{idle, hit, hpAnim, dmgAnim}) if (a != null) a.cancel();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int w, int h) {
        setMeasuredDimension(MeasureSpec.getSize(w), Math.round(210 * d));
    }

    private int blend(int base) {
        int r = (int) (((base >> 16) & 0xFF) * (1 - flash) + 255 * flash);
        int g = (int) (((base >> 8) & 0xFF) * (1 - flash) + 82 * flash);
        int b = (int) ((base & 0xFF) * (1 - flash) + 82 * flash);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void fill(int color) {
        p.setStyle(Paint.Style.FILL);
        p.setShader(null);
        p.setColor(blend(color));
        if (defeated) p.setAlpha(110);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), pad = 14 * d;

        // arena
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, 0, w, h, 0xFF1B1F3B, 0xFF3A1B4E, Shader.TileMode.CLAMP));
        rect.set(0, 0, w, h);
        c.drawRoundRect(rect, 20 * d, 20 * d, p);
        p.setShader(null);

        // nama + bar HP
        t.setColor(0xFFFFFFFF);
        t.setTextSize(15 * d);
        t.setTextAlign(Paint.Align.LEFT);
        c.drawText(title + "  (" + BossSystem.kindName(kind) + ")", pad, pad + 14 * d, t);
        float by = pad + 24 * d, bh = 12 * d;
        p.setColor(0x33FFFFFF);
        rect.set(pad, by, w - pad, by + bh);
        c.drawRoundRect(rect, bh / 2, bh / 2, p);
        if (hp > 0f) {
            p.setShader(new LinearGradient(pad, 0, w - pad, 0, 0xFFFF5252, 0xFFFF8A65, Shader.TileMode.CLAMP));
            rect.set(pad, by, pad + (w - 2 * pad) * hp, by + bh);
            c.drawRoundRect(rect, bh / 2, bh / 2, p);
            p.setShader(null);
        }
        t.setTextSize(11 * d);
        t.setColor(0xCCFFFFFF);
        c.drawText("HP " + Money.rp(Math.max(0, target - saved)) + " / " + Money.rp(target), pad, by + bh + 14 * d, t);

        // boss
        float cx = w / 2f + shake, cy = h * 0.66f + bob;
        float s = Math.min(w * 0.26f, h * 0.30f);
        switch (((kind % 3) + 3) % 3) {
            case 0: slime(c, cx, cy, s); break;
            case 1: golem(c, cx, cy, s); break;
            default: dragon(c, cx, cy, s); break;
        }

        // tebasan
        if (slash > 0f && slash < 1f) {
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeWidth(5 * d);
            p.setColor(0xFFFFFFFF);
            p.setAlpha((int) (255 * (1f - slash)));
            float k = Math.min(1f, slash * 1.7f);
            float x0 = cx - s * 1.3f, y0 = cy - s * 1.3f, x1 = cx + s * 1.3f, y1 = cy + s * 0.9f;
            c.drawLine(x0, y0, x0 + (x1 - x0) * k, y0 + (y1 - y0) * k, p);
            p.setStyle(Paint.Style.FILL);
        }

        // angka damage
        if (dmgT < 1f) {
            t.setTextSize(20 * d);
            t.setTextAlign(Paint.Align.CENTER);
            t.setColor(0xFFFFEB3B);
            t.setAlpha((int) (255 * (1f - dmgT)));
            c.drawText(dmgText, cx + s * 0.5f, cy - s * 1.05f - dmgT * 40 * d, t);
            t.setAlpha(255);
        }

        // boss kalah
        if (defeated) {
            c.save();
            c.rotate(-10f, w / 2f, h * 0.55f);
            t.setTextAlign(Paint.Align.CENTER);
            t.setTextSize(26 * d);
            t.setColor(0xFFFFCA28);
            c.drawText("BOSS KALAH", w / 2f, h * 0.58f, t);
            c.restore();
        }
    }

    private void eyes(Canvas c, float lx, float rx, float y, float r, int color) {
        if (defeated) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(2.5f * d);
            p.setColor(0xFFFFFFFF);
            for (float x : new float[]{lx, rx}) {
                c.drawLine(x - r, y - r, x + r, y + r, p);
                c.drawLine(x - r, y + r, x + r, y - r, p);
            }
            p.setStyle(Paint.Style.FILL);
            return;
        }
        fill(0xFFFFFFFF);
        c.drawCircle(lx, y, r, p);
        c.drawCircle(rx, y, r, p);
        fill(color);
        c.drawCircle(lx, y, r * 0.5f, p);
        c.drawCircle(rx, y, r * 0.5f, p);
    }

    private void slime(Canvas c, float cx, float cy, float s) {
        path.reset();
        path.moveTo(cx - s, cy + s * 0.7f);
        path.cubicTo(cx - s, cy - s * 1.1f, cx + s, cy - s * 1.1f, cx + s, cy + s * 0.7f);
        path.close();
        fill(0xFF66BB6A);
        c.drawPath(path, p);
        fill(0x55FFFFFF);
        rect.set(cx - s * 0.65f, cy - s * 0.55f, cx - s * 0.2f, cy - s * 0.25f);
        c.drawOval(rect, p);
        eyes(c, cx - s * 0.35f, cx + s * 0.35f, cy - s * 0.05f, s * 0.2f, 0xFF1B5E20);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2.5f * d);
        p.setColor(0xFF1B5E20);
        rect.set(cx - s * 0.25f, cy + s * 0.12f, cx + s * 0.25f, cy + s * 0.5f);
        c.drawArc(rect, 20, 140, false, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void golem(Canvas c, float cx, float cy, float s) {
        fill(0xFF6D4C41);
        rect.set(cx - s * 0.75f, cy - s * 0.35f, cx + s * 0.75f, cy + s * 0.75f);
        c.drawRoundRect(rect, 8 * d, 8 * d, p);
        rect.set(cx - s * 1.1f, cy - s * 0.25f, cx - s * 0.8f, cy + s * 0.6f);
        c.drawRoundRect(rect, 6 * d, 6 * d, p);
        rect.set(cx + s * 0.8f, cy - s * 0.25f, cx + s * 1.1f, cy + s * 0.6f);
        c.drawRoundRect(rect, 6 * d, 6 * d, p);
        fill(0xFF8D6E63);
        rect.set(cx - s * 0.5f, cy - s * 1.0f, cx + s * 0.5f, cy - s * 0.3f);
        c.drawRoundRect(rect, 8 * d, 8 * d, p);
        eyes(c, cx - s * 0.25f, cx + s * 0.25f, cy - s * 0.65f, s * 0.13f, 0xFFFF6F00);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(2f * d);
        p.setColor(0xFF3E2723);
        c.drawLine(cx - s * 0.3f, cy + s * 0.05f, cx - s * 0.1f, cy + s * 0.4f, p);
        c.drawLine(cx + s * 0.25f, cy, cx + s * 0.4f, cy + s * 0.5f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void dragon(Canvas c, float cx, float cy, float s) {
        // sayap
        fill(0xFF7B1FA2);
        path.reset();
        path.moveTo(cx - s * 0.5f, cy - s * 0.1f);
        path.lineTo(cx - s * 1.5f, cy - s * 0.9f);
        path.lineTo(cx - s * 1.1f, cy + s * 0.3f);
        path.close();
        c.drawPath(path, p);
        path.reset();
        path.moveTo(cx + s * 0.5f, cy - s * 0.1f);
        path.lineTo(cx + s * 1.5f, cy - s * 0.9f);
        path.lineTo(cx + s * 1.1f, cy + s * 0.3f);
        path.close();
        c.drawPath(path, p);
        // badan + kepala
        fill(0xFFAB47BC);
        c.drawCircle(cx, cy + s * 0.1f, s * 0.85f, p);
        // tanduk
        fill(0xFFFFE082);
        path.reset();
        path.moveTo(cx - s * 0.55f, cy - s * 0.55f);
        path.lineTo(cx - s * 0.8f, cy - s * 1.15f);
        path.lineTo(cx - s * 0.25f, cy - s * 0.7f);
        path.close();
        c.drawPath(path, p);
        path.reset();
        path.moveTo(cx + s * 0.55f, cy - s * 0.55f);
        path.lineTo(cx + s * 0.8f, cy - s * 1.15f);
        path.lineTo(cx + s * 0.25f, cy - s * 0.7f);
        path.close();
        c.drawPath(path, p);
        eyes(c, cx - s * 0.33f, cx + s * 0.33f, cy - s * 0.1f, s * 0.17f, 0xFFD50000);
        // gigi
        fill(0xFFFFFFFF);
        path.reset();
        path.moveTo(cx - s * 0.3f, cy + s * 0.45f);
        path.lineTo(cx - s * 0.2f, cy + s * 0.7f);
        path.lineTo(cx - s * 0.1f, cy + s * 0.45f);
        path.moveTo(cx + s * 0.3f, cy + s * 0.45f);
        path.lineTo(cx + s * 0.2f, cy + s * 0.7f);
        path.lineTo(cx + s * 0.1f, cy + s * 0.45f);
        c.drawPath(path, p);
    }
}
