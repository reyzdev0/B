package com.savequest.app.update;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.pm.PackageInfoCompat;

import com.savequest.app.data.backup.BackupManager;
import com.savequest.app.util.Session;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pembaruan OTA tanpa uninstall:
 *  1. Snapshot data ke file pengaman (jaga-jaga).
 *  2. Unduh APK baru ke cache.
 *  3. Pastikan paketnya sama + versinya lebih tinggi.
 *  4. Pasang lewat PackageInstaller session. Android menimpa app lama; data internal
 *     (database Room, pengaturan) tetap utuh karena package name dan tanda tangan sama.
 */
public final class UpdateInstaller {
    private UpdateInstaller() {}

    public interface Progress {
        void onStage(String text);
        void onProgress(int percent);
        void onDone(boolean ok, String message);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void downloadAndInstall(Context ctx, UpdateChecker.Info info, Progress cb) {
        final Context app = ctx.getApplicationContext();
        IO.execute(() -> {
            try {
                stage(cb, "Mengamankan data kamu...");
                BackupManager.snapshotNow(app);

                stage(cb, "Mengunduh pembaruan...");
                File apk = download(app, info, cb);

                stage(cb, "Memeriksa berkas...");
                verify(app, apk);

                stage(cb, "Memasang pembaruan...");
                Session.markExternal();
                install(app, apk);
                MAIN.post(() -> cb.onDone(true, "Ikuti konfirmasi pemasangan dari sistem Android"));
            } catch (Exception e) {
                String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                MAIN.post(() -> cb.onDone(false, "Pembaruan gagal: " + msg));
            }
        });
    }

    private static void stage(Progress cb, String text) {
        MAIN.post(() -> cb.onStage(text));
    }

    private static File download(Context app, UpdateChecker.Info info, Progress cb) throws IOException {
        File dir = new File(app.getCacheDir(), "update");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Folder cache gagal dibuat");
        File out = new File(dir, "update.apk");
        if (out.exists()) out.delete();

        HttpURLConnection c = (HttpURLConnection) new URL(info.apkUrl).openConnection();
        c.setConnectTimeout(15_000);
        c.setReadTimeout(30_000);
        c.setRequestProperty("User-Agent", "SaveQuest-Updater");
        try {
            if (c.getResponseCode() != 200) throw new IOException("Unduhan gagal (HTTP " + c.getResponseCode() + ")");
            long total = c.getContentLengthLong() > 0 ? c.getContentLengthLong() : info.size;
            try (InputStream in = c.getInputStream(); OutputStream os = new FileOutputStream(out)) {
                byte[] buf = new byte[16 * 1024];
                long done = 0;
                int last = -1, n;
                while ((n = in.read(buf)) > 0) {
                    os.write(buf, 0, n);
                    done += n;
                    if (total > 0) {
                        int pct = (int) (done * 100 / total);
                        if (pct != last) {
                            last = pct;
                            final int p = pct;
                            MAIN.post(() -> cb.onProgress(p));
                        }
                    }
                }
            }
        } finally {
            c.disconnect();
        }
        if (out.length() < 100_000) throw new IOException("Berkas unduhan tidak valid");
        return out;
    }

    private static void verify(Context app, File apk) throws IOException {
        PackageInfo archive = app.getPackageManager().getPackageArchiveInfo(apk.getAbsolutePath(), 0);
        if (archive == null) throw new IOException("APK rusak atau tidak bisa dibaca");
        if (!app.getPackageName().equals(archive.packageName)) {
            throw new IOException("APK bukan milik SaveQuest");
        }
        if (PackageInfoCompat.getLongVersionCode(archive) <= UpdateChecker.installedVersionCode(app)) {
            throw new IOException("APK tidak lebih baru dari versi terpasang");
        }
    }

    private static void install(Context app, File apk) throws IOException {
        PackageInstaller installer = app.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params =
                new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(app.getPackageName());
        if (Build.VERSION.SDK_INT >= 31) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
        }
        int id = installer.createSession(params);
        try (PackageInstaller.Session session = installer.openSession(id)) {
            try (InputStream in = new FileInputStream(apk);
                 OutputStream out = session.openWrite("SaveQuest.apk", 0, apk.length())) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                session.fsync(out);
            }
            Intent result = new Intent(app, InstallResultReceiver.class);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 31) flags |= PendingIntent.FLAG_MUTABLE;
            PendingIntent pi = PendingIntent.getBroadcast(app, id, result, flags);
            session.commit(pi.getIntentSender());
        }
    }
}
