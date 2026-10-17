package com.savequest.app.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.savequest.app.R;
import com.savequest.app.ui.common.Ui;
import com.savequest.app.util.DateUtil;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Kalender kontribusi ala GitHub: kolom = minggu (Senin-Minggu), baris = hari.
 * Warna makin pekat sesuai nominal setoran hari itu. Ketuk kotak untuk detail.
 */
public class HeatmapView extends View {
    public interface OnCellClick {
        void onClick(long day, long total);
    }

    private final int weeks;
    private final Map<Long, Long> data = new HashMap<>();
    private final int[] heat = new int[5];
    private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float gap, leftW, topH, legendH;
    private float cell;
    private long endDay = DateUtil.today();
    private OnCellClick listener;

    public HeatmapView(Context c) { this(c, null); }

    public HeatmapView(Context c, AttributeSet a) {
        super(c, a);
        weeks = 21;
        gap = Ui.dp(c, 3);
        leftW = Ui.dp(c, 26);
        topH = Ui.dp(c, 18);
        legendH = Ui.dp(c, 26);
        int[] ids = {R.color.sq_heat_0, R.color.sq_heat_1, R.color.sq_heat_2, R.color.sq_heat_3, R.color.sq_heat_4};
        for (int i = 0; i < 5; i++) heat[i] = ContextCompat.getColor(c, ids[i]);
        textPaint.setColor(Ui.color(c, R.color.sq_text_dim));
        textPaint.setTextSize(Ui.dp(c, 10));
    }

    public void setOnCellClick(OnCellClick l) { listener = l; }

    public void setData(Map<Long, Long> totals) {
        data.clear();
        data.putAll(totals);
        endDay = DateUtil.today();
        invalidate();
    }

    /** 0 = kosong, 1..4 makin pekat. */
    public static int level(long total) {
        if (total <= 0) return 0;
        if (total <= 5_000) return 1;
        if (total <= 20_000) return 2;
        if (total <= 50_000) return 3;
        return 4;
    }

    private long startDay() {
        return DateUtil.weekStart(endDay) - (weeks - 1) * 7L;
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        cell = (w - leftW) / weeks - gap;
        int h = (int) (topH + 7 * (cell + gap) + legendH);
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas c) {
        long start = startDay();
        String[] dayNames = {"Sen", "", "Rab", "", "Jum", "", "Min"};
        for (int r = 0; r < 7; r++) {
            if (!dayNames[r].isEmpty()) {
                c.drawText(dayNames[r], 0, topH + r * (cell + gap) + cell * 0.8f, textPaint);
            }
        }
        int lastMonth = -1;
        for (int col = 0; col < weeks; col++) {
            float x = leftW + col * (cell + gap);
            LocalDate first = LocalDate.ofEpochDay(start + col * 7L);
            if (first.getMonthValue() != lastMonth && first.getDayOfMonth() <= 7) {
                c.drawText(first.getMonth().getDisplayName(TextStyle.SHORT, new Locale("id", "ID")),
                        x, topH - Ui.dp(getContext(), 5), textPaint);
                lastMonth = first.getMonthValue();
            }
            for (int row = 0; row < 7; row++) {
                long day = start + col * 7L + row;
                if (day > endDay) continue;
                Long total = data.get(day);
                cellPaint.setColor(heat[level(total == null ? 0 : total)]);
                float y = topH + row * (cell + gap);
                rect.set(x, y, x + cell, y + cell);
                c.drawRoundRect(rect, cell * 0.2f, cell * 0.2f, cellPaint);
            }
        }

        // legenda
        float ly = topH + 7 * (cell + gap) + Ui.dp(getContext(), 8);
        float size = Ui.dp(getContext(), 11);
        float lx = getWidth() - 5 * (size + gap) - Ui.dp(getContext(), 34);
        c.drawText("Sedikit", lx - Ui.dp(getContext(), 40), ly + size - 1, textPaint);
        for (int i = 0; i < 5; i++) {
            cellPaint.setColor(heat[i]);
            rect.set(lx + i * (size + gap), ly, lx + i * (size + gap) + size, ly + size);
            c.drawRoundRect(rect, 3, 3, cellPaint);
        }
        c.drawText("Banyak", lx + 5 * (size + gap) + 4, ly + size - 1, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP && listener != null) {
            int col = (int) ((e.getX() - leftW) / (cell + gap));
            int row = (int) ((e.getY() - topH) / (cell + gap));
            if (in0(weeks, col) && row >= 0 && row < 7) {
                long day = startDay() + col * 7L + row;
                if (day <= endDay) {
                    Long total = data.get(day);
                    listener.onClick(day, total == null ? 0 : total);
                }
            }
            return true;
        }
        return e.getAction() == MotionEvent.ACTION_DOWN;
    }

    private static boolean in0(int max, int v) { return v >= 0 && v < max; }
}
