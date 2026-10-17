package com.savequest.app.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.savequest.app.ui.common.Ui;

/**
 * Slot item bergaya RPG (Canvas): bingkai warna rarity, ikon di tengah, lencana jumlah
 * "x3" di pojok kanan bawah, dan cahaya saat dipilih. Dipakai di grid Ransel dan daftar Toko.
 */
public class ItemSlotView extends View {
    private int iconRes;
    private int rarityColor = 0xFF90A4AE;
    private int quantity = -1;
    private boolean selected;
    private boolean dimmed;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public ItemSlotView(Context c) { this(c, null); }

    public ItemSlotView(Context c, AttributeSet a) {
        super(c, a);
        stroke.setStyle(Paint.Style.STROKE);
        text.setColor(0xFFFFFFFF);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
    }

    /** quantity &lt; 0 menyembunyikan lencana jumlah. */
    public void set(int iconRes, int rarityColor, int quantity, boolean selected, boolean dimmed) {
        this.iconRes = iconRes;
        this.rarityColor = rarityColor;
        this.quantity = quantity;
        this.selected = selected;
        this.dimmed = dimmed;
        invalidate();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int size = MeasureSpec.getMode(wSpec) == MeasureSpec.UNSPECIFIED
                ? Ui.dp(getContext(), 72) : MeasureSpec.getSize(wSpec);
        setMeasuredDimension(size, size);
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float d = getResources().getDisplayMetrics().density;
        float radius = 14 * d;
        float inset = 3 * d;

        // cahaya saat dipilih
        if (selected) {
            stroke.setShader(null);
            stroke.setColor((rarityColor & 0x00FFFFFF) | 0x66000000);
            stroke.setStrokeWidth(6 * d);
            rect.set(inset, inset, w - inset, h - inset);
            c.drawRoundRect(rect, radius, radius, stroke);
        }

        // latar gradient mengikuti rarity
        int top = (rarityColor & 0x00FFFFFF) | 0x55000000;
        int bottom = (rarityColor & 0x00FFFFFF) | 0x1A000000;
        fill.setShader(new LinearGradient(0, 0, 0, h, top, bottom, Shader.TileMode.CLAMP));
        rect.set(inset, inset, w - inset, h - inset);
        c.drawRoundRect(rect, radius, radius, fill);
        fill.setShader(null);

        // bingkai
        stroke.setColor(rarityColor);
        stroke.setStrokeWidth((selected ? 3 : 2) * d);
        c.drawRoundRect(rect, radius, radius, stroke);

        // ikon
        Drawable icon = ContextCompat.getDrawable(getContext(), iconRes);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(dimmed ? 0x66FFFFFF & rarityColor | 0x55000000 : rarityColor);
            float s = w * 0.27f;
            icon.setBounds(Math.round(w / 2 - s), Math.round(h / 2 - s), Math.round(w / 2 + s), Math.round(h / 2 + s));
            icon.draw(c);
        }

        // lencana jumlah
        if (quantity >= 0) {
            String label = "x" + quantity;
            text.setTextSize(11 * d);
            float tw = text.measureText(label);
            float bw = tw + 10 * d, bh = 17 * d;
            float right = w - inset - 2 * d, bottomY = h - inset - 2 * d;
            fill.setColor(0xDD1B2430);
            rect.set(right - bw, bottomY - bh, right, bottomY);
            c.drawRoundRect(rect, bh / 2, bh / 2, fill);
            c.drawText(label, rect.centerX(), rect.centerY() + 4 * d, text);
        }
    }
}
