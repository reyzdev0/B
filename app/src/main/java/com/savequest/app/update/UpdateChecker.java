package com.savequest.app.update;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.pm.PackageInfoCompat;

import com.savequest.app.util.AppPrefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Memeriksa versi terbaru. Dua format sumber yang didukung:
 *  1. GitHub Releases: "pemilik/repo" (memakai /releases/latest, repo harus publik).
 *     Isi rilis memuat baris "versionCode: N" (dibuat otomatis oleh workflow).
 *  2. JSON kustom (URL lengkap): {"versionCode":1005,"versionName":"2.2.5","apkUrl":"https://...","notes":"..."}
 */
public final class UpdateChecker {
    private UpdateChecker() {}

    public static final class Info {
        public long versionCode;
        public String versionName;
        public String notes = "";
        public String apkUrl;
        public long size;
    }

    public static final class Result {
        public static final int UPDATE = 0, UP_TO_DATE = 1, ERROR = 2, NO_SOURCE = 3;
        public int status;
        public Info info;
        public String message;
    }

    public interface Callback {
        void onResult(Result result);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static String sourceUrl(String source) {
        if (source == null) return "";
        String s = source.trim();
        if (s.isEmpty()) return "";
        if (s.startsWith("http://") || s.startsWith("https://")) return s;
        if (s.matches("[\\w.\\-]+/[\\w.\\-]+")) return "https://api.github.com/repos/" + s + "/releases/latest";
        return "";
    }

    public static long installedVersionCode(Context c) {
        try {
            PackageInfo pi = c.getPackageManager().getPackageInfo(c.getPackageName(), 0);
            return PackageInfoCompat.getLongVersionCode(pi);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String installedVersionName(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    public static void check(Context ctx, Callback cb) {
        final Context app = ctx.getApplicationContext();
        final AppPrefs prefs = new AppPrefs(app);
        final String url = sourceUrl(prefs.updateSource());
        if (url.isEmpty()) {
            Result r = new Result();
            r.status = Result.NO_SOURCE;
            r.message = "Sumber update belum diatur";
            MAIN.post(() -> cb.onResult(r));
            return;
        }
        IO.execute(() -> {
            Result r = new Result();
            try {
                Info info = parse(httpGet(url));
                prefs.setLastUpdateCheck(System.currentTimeMillis());
                if (info == null) {
                    r.status = Result.ERROR;
                    r.message = "Format data update tidak dikenali";
                } else if (info.versionCode > installedVersionCode(app)) {
                    r.status = Result.UPDATE;
                    r.info = info;
                } else {
                    r.status = Result.UP_TO_DATE;
                    r.message = "Kamu sudah memakai versi terbaru";
                }
            } catch (Exception e) {
                r.status = Result.ERROR;
                r.message = e.getMessage() == null ? "Gagal memeriksa pembaruan" : e.getMessage();
            }
            MAIN.post(() -> cb.onResult(r));
        });
    }

    static String httpGet(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(10_000);
        c.setReadTimeout(15_000);
        c.setRequestProperty("User-Agent", "SaveQuest-Updater");
        c.setRequestProperty("Accept", "application/vnd.github+json");
        try {
            int code = c.getResponseCode();
            if (code == 404) throw new IOException("Rilis tidak ditemukan (repo salah, belum ada rilis, atau repo privat)");
            if (code == 403) throw new IOException("Akses ditolak / batas permintaan GitHub tercapai. Coba lagi nanti.");
            if (code != 200) throw new IOException("Server membalas HTTP " + code);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append('\n');
            }
            return sb.toString();
        } finally {
            c.disconnect();
        }
    }

    static Info parse(String json) throws Exception {
        JSONObject o = new JSONObject(json);
        Info info = new Info();
        if (o.has("tag_name")) { // GitHub Releases
            String body = o.optString("body", "");
            String tag = o.optString("tag_name", "");
            Matcher vc = Pattern.compile("versionCode:\\s*(\\d+)").matcher(body);
            if (vc.find()) {
                info.versionCode = Long.parseLong(vc.group(1));
            } else {
                Matcher n = Pattern.compile("(\\d+)$").matcher(tag);
                if (!n.find()) return null;
                info.versionCode = 1000 + Long.parseLong(n.group(1));
            }
            Matcher vn = Pattern.compile("versionName:\\s*([\\w.\\-]+)").matcher(body);
            info.versionName = vn.find() ? vn.group(1) : tag;
            info.notes = body.replaceAll("(?m)^(versionCode|versionName):.*$", "").trim();
            JSONArray assets = o.optJSONArray("assets");
            for (int i = 0; assets != null && i < assets.length(); i++) {
                JSONObject a = assets.getJSONObject(i);
                if (a.optString("name", "").endsWith(".apk")) {
                    info.apkUrl = a.optString("browser_download_url", null);
                    info.size = a.optLong("size");
                    break;
                }
            }
        } else { // JSON kustom
            info.versionCode = o.optLong("versionCode");
            info.versionName = o.optString("versionName", String.valueOf(info.versionCode));
            info.apkUrl = o.optString("apkUrl", null);
            info.notes = o.optString("notes", "");
            info.size = o.optLong("size");
        }
        if (info.apkUrl == null || info.apkUrl.isEmpty() || info.versionCode <= 0) return null;
        return info;
    }
}
