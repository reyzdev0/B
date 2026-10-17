package com.savequest.app.ui.avatar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** Mengubah foto galeri menjadi avatar persegi 512x512 yang disimpan di penyimpanan internal app. */
public final class AvatarImageUtil {
    private AvatarImageUtil() {}

    /** Jalankan di background thread. Mengembalikan path file hasil. */
    public static String saveSquare(Context c, Uri uri) throws IOException {
        Bitmap src;
        if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.Source source = ImageDecoder.createSource(c.getContentResolver(), uri);
            src = ImageDecoder.decodeBitmap(source, (decoder, info, s) -> {
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                int w = info.getSize().getWidth(), h = info.getSize().getHeight();
                int min = Math.min(w, h);
                if (min > 1024) {
                    float k = 1024f / min;
                    decoder.setTargetSize(Math.round(w * k), Math.round(h * k));
                }
            });
        } else {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream is = c.getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(is, null, o);
            }
            int sample = 1;
            while (Math.min(o.outWidth, o.outHeight) / sample > 1024) sample *= 2;
            o.inJustDecodeBounds = false;
            o.inSampleSize = sample;
            try (InputStream is = c.getContentResolver().openInputStream(uri)) {
                src = BitmapFactory.decodeStream(is, null, o);
            }
        }
        if (src == null) throw new IOException("Gambar tidak bisa dibaca");

        int w = src.getWidth(), h = src.getHeight(), side = Math.min(w, h);
        Bitmap square = Bitmap.createBitmap(src, (w - side) / 2, (h - side) / 2, side, side);
        Bitmap out = Bitmap.createScaledBitmap(square, 512, 512, true);

        File dir = new File(c.getFilesDir(), "avatars");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Folder avatar gagal dibuat");
        File[] old = dir.listFiles();
        if (old != null) for (File f : old) f.delete();
        File file = new File(dir, "avatar_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            out.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        }
        return file.getAbsolutePath();
    }
}
