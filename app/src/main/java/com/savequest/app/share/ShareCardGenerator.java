package com.savequest.app.share;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.savequest.app.R;
import com.savequest.app.data.local.entity.BadgeEntity;
import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.domain.BadgeCatalog;
import com.savequest.app.domain.LevelSystem;
import com.savequest.app.domain.StreakSystem;
import com.savequest.app.ui.common.Ui;
import com.savequest.app.ui.views.AvatarRenderer;
import com.savequest.app.util.DateUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Membuat kartu pencapaian 1080x1920 (rasio Story IG / Status WA) dan membagikannya. */
public final class ShareCardGenerator {
    private ShareCardGenerator() {}

    public static final int W = 1080, H = 1920;

    public static Bitmap render(Context c, PlayerEntity p, List<BadgeEntity> badges) {
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int level = LevelSystem.levelFor(p.totalExp);
        int streak = StreakSystem.effective(p.streak, p.lastDepositDay, DateUtil.today());

        // latar + cahaya
        paint.setShader(new LinearGradient(0, 0, W, H, new int[]{0xFF0B0F1E, 0xFF2A1B5E, 0xFF0A3A4A}, null, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, W, H, paint);
        paint.setShader(new RadialGradient(900, 260, 520, 0x5500E5FF, 0x0000E5FF, Shader.TileMode.CLAMP));
        cv.drawCircle(900, 260, 520, paint);
        paint.setShader(new RadialGradient(120, 1550, 620, 0x558E7CFF, 0x008E7CFF, Shader.TileMode.CLAMP));
        cv.drawCircle(120, 1550, 620, paint);
        paint.setShader(null);

        // judul
        Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        text.setTextAlign(Paint.Align.CENTER);
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setColor(0xFFFFFFFF);
        text.setLetterSpacing(0.3f);
        text.setTextSize(64);
        cv.drawText("SAVEQUEST", W / 2f, 170, text);
        text.setLetterSpacing(0f);
        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0xFF9AA7C7);
        text.setTextSize(36);
        cv.drawText("Nabung sambil main RPG", W / 2f, 232, text);

        // avatar
        AvatarRenderer.draw(c, cv, W / 2f, 640, 310, AvatarRenderer.Appearance.from(p));

        // level
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setColor(0xFFFFCA28);
        text.setTextSize(124);
        cv.drawText("Lv " + level, W / 2f, 1085, text);
        text.setColor(0xFFFFFFFF);
        text.setTextSize(54);
        cv.drawText(LevelSystem.title(level), W / 2f, 1160, text);

        // tiga statistik
        String[] vals = {String.valueOf(streak), String.valueOf(level), String.valueOf(unlockedCount(badges))};
        String[] labs = {"hari streak", "level", "badge"};
        int[] icons = {R.drawable.ic_flame, R.drawable.ic_bolt, R.drawable.ic_trophy};
        int[] tints = {0xFFFF7A1A, 0xFFFFCA28, 0xFFB39DFF};
        float bw = 300, gap = 45, x0 = (W - (3 * bw + 2 * gap)) / 2f;
        for (int i = 0; i < 3; i++) {
            float x = x0 + i * (bw + gap);
            paint.setColor(0x22FFFFFF);
            cv.drawRoundRect(new RectF(x, 1240, x + bw, 1470), 40, 40, paint);
            icon(c, cv, icons[i], x + bw / 2f, 1303, 64, tints[i]);
            text.setTypeface(Typeface.DEFAULT_BOLD);
            text.setColor(0xFFFFFFFF);
            text.setTextSize(82);
            cv.drawText(vals[i], x + bw / 2f, 1405, text);
            text.setTypeface(Typeface.DEFAULT);
            text.setColor(0xFF9AA7C7);
            text.setTextSize(30);
            cv.drawText(labs[i], x + bw / 2f, 1450, text);
        }

        // baris badge
        text.setTypeface(Typeface.DEFAULT_BOLD);
        text.setColor(0xFFFFFFFF);
        text.setTextSize(40);
        cv.drawText("Badge Terbuka", W / 2f, 1570, text);
        List<BadgeCatalog.Def> shown = unlockedDefs(badges, 6);
        if (shown.isEmpty()) {
            text.setTypeface(Typeface.DEFAULT);
            text.setColor(0xFF9AA7C7);
            text.setTextSize(34);
            cv.drawText("Perjalanan baru dimulai", W / 2f, 1680, text);
        } else {
            float r = 72, g = 28;
            float total = shown.size() * 2 * r + (shown.size() - 1) * g;
            float cx = (W - total) / 2f + r;
            for (BadgeCatalog.Def d : shown) {
                int[] col = Ui.tier(d.tier);
                paint.setShader(new LinearGradient(cx - r, 1690 - r, cx + r, 1690 + r, col[0], col[1], Shader.TileMode.CLAMP));
                cv.drawCircle(cx, 1690, r, paint);
                paint.setShader(null);
                icon(c, cv, d.iconRes, cx, 1690, 80, 0xFFFFFFFF);
                cx += 2 * r + g;
            }
        }

        text.setTypeface(Typeface.DEFAULT);
        text.setColor(0x889AA7C7);
        text.setTextSize(32);
        cv.drawText("#SaveQuest", W / 2f, 1850, text);
        return bmp;
    }

    private static int unlockedCount(List<BadgeEntity> list) {
        int n = 0;
        if (list != null) for (BadgeEntity b : list) if (b.unlockedAt > 0) n++;
        return n;
    }

    private static List<BadgeCatalog.Def> unlockedDefs(List<BadgeEntity> list, int max) {
        List<BadgeCatalog.Def> out = new ArrayList<>();
        if (list == null) return out;
        for (BadgeCatalog.Def d : BadgeCatalog.ALL) {
            for (BadgeEntity b : list) {
                if (b.code.equals(d.code) && b.unlockedAt > 0) {
                    out.add(d);
                    break;
                }
            }
            if (out.size() >= max) break;
        }
        return out;
    }

    private static void icon(Context c, Canvas cv, int res, float cx, float cy, float size, int tint) {
        Drawable d = ContextCompat.getDrawable(c, res);
        if (d == null) return;
        d = d.mutate();
        d.setTint(tint);
        int h = Math.round(size / 2f);
        d.setBounds(Math.round(cx) - h, Math.round(cy) - h, Math.round(cx) + h, Math.round(cy) + h);
        d.draw(cv);
    }

    /** Simpan PNG ke cache dan kembalikan content:// Uri (FileProvider). */
    public static Uri save(Context c, Bitmap bmp) throws IOException {
        File dir = new File(c.getCacheDir(), "share");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Folder cache gagal dibuat");
        File f = new File(dir, "savequest_card.png");
        try (FileOutputStream fos = new FileOutputStream(f)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        return FileProvider.getUriForFile(c, c.getPackageName() + ".fileprovider", f);
    }

    public static Intent chooser(Context c, Uri uri) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("image/png");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.putExtra(Intent.EXTRA_TEXT, "Progres nabungku di SaveQuest!");
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        return Intent.createChooser(send, "Bagikan kartu pencapaian");
    }
}
