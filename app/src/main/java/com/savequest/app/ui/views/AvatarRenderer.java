package com.savequest.app.ui.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.domain.AvatarCatalog;
import com.savequest.app.domain.CosmeticCatalog;
import com.savequest.app.domain.LevelSystem;
import com.savequest.app.ui.common.Ui;

import java.io.File;

/**
 * Menggambar avatar lengkap: latar -> karakter (+ skin) -> bingkai.
 * Dipakai AvatarView di layar dan kartu share (Canvas bitmap).
 */
public final class AvatarRenderer {
    private AvatarRenderer() {}

    public static final class Appearance {
        public int avatarRes;
        public Bitmap bitmap;
        public String bgId = CosmeticCatalog.BG_DEFAULT;
        public String frameId = CosmeticCatalog.FRAME_NONE;
        public String skinId = CosmeticCatalog.SKIN_DEFAULT;
        public int tier = 1;

        public Appearance copy() {
            Appearance a = new Appearance();
            a.avatarRes = avatarRes;
            a.bitmap = bitmap;
            a.bgId = bgId;
            a.frameId = frameId;
            a.skinId = skinId;
            a.tier = tier;
            return a;
        }

        public static Appearance from(PlayerEntity p) {
            Appearance a = new Appearance();
            int level = LevelSystem.levelFor(p.totalExp);
            a.tier = LevelSystem.tier(level);
            a.avatarRes = AvatarCatalog.drawableFor(p.avatarId, level);
            if (AvatarCatalog.CUSTOM.equals(p.avatarId)) a.bitmap = loadCustom(p.customAvatarPath);
            if (p.bgId != null) a.bgId = p.bgId;
            if (p.frameId != null) a.frameId = p.frameId;
            if (p.skinId != null) a.skinId = p.skinId;
            return a;
        }
    }

    private static Bitmap cacheBitmap;
    private static String cachePath;

    public static Bitmap loadCustom(String path) {
        if (path == null) return null;
        if (path.equals(cachePath) && cacheBitmap != null) return cacheBitmap;
        if (!new File(path).exists()) return null;
        Bitmap b = BitmapFactory.decodeFile(path);
        if (b != null) {
            cacheBitmap = b;
            cachePath = path;
        }
        return b;
    }

    public static void draw(Context c, Canvas cv, float cx, float cy, float r, Appearance a) {
        float ringW = r * 0.10f;
        float inner = r - ringW;
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

        // 1. latar
        CosmeticCatalog.Item bg = CosmeticCatalog.find(a.bgId);
        int b1 = bg != null ? bg.c1 : 0xFFB0BEC5, b2 = bg != null ? bg.c2 : 0xFF78909C;
        paint.setShader(new LinearGradient(cx - inner, cy - inner, cx + inner, cy + inner, b1, b2, Shader.TileMode.CLAMP));
        cv.drawCircle(cx, cy, inner, paint);
        paint.setShader(null);

        // 2. karakter (dipotong bulat)
        cv.save();
        Path clip = new Path();
        clip.addCircle(cx, cy, inner, Path.Direction.CW);
        cv.clipPath(clip);
        if (a.bitmap != null) {
            cv.drawBitmap(a.bitmap, null, new RectF(cx - inner, cy - inner, cx + inner, cy + inner), paint);
        } else {
            Drawable d = ContextCompat.getDrawable(c, a.avatarRes);
            if (d != null) {
                d = d.mutate();
                float s = inner * 1.05f;
                d.setBounds(Math.round(cx - s), Math.round(cy - s), Math.round(cx + s), Math.round(cy + s));
                d.setColorFilter(CosmeticCatalog.skinFilter(a.skinId));
                d.draw(cv);
            }
        }
        cv.restore();

        // 3. bingkai: item toko, atau warna tier level jika belum memakai
        CosmeticCatalog.Item fr = CosmeticCatalog.find(a.frameId);
        int c1, c2, c3;
        if (fr != null && !CosmeticCatalog.FRAME_NONE.equals(fr.id)) {
            c1 = fr.c1; c2 = fr.c2; c3 = fr.c3;
        } else {
            int[] t = Ui.tier(a.tier);
            c1 = t[0]; c2 = t[1]; c3 = t[0];
        }
        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(ringW);
        ring.setShader(new SweepGradient(cx, cy, new int[]{c1, c2, c3, c1}, null));
        cv.drawCircle(cx, cy, r - ringW / 2f, ring);
    }
}
