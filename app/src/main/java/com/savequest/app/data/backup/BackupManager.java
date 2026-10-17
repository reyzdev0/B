package com.savequest.app.data.backup;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.savequest.app.data.local.AppDatabase;
import com.savequest.app.data.local.entity.BadgeEntity;
import com.savequest.app.data.local.entity.InventoryEntity;
import com.savequest.app.data.local.entity.OwnedItemEntity;
import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.data.local.entity.QuestEntity;
import com.savequest.app.data.local.entity.TransactionEntity;
import com.savequest.app.data.local.entity.VaultEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Export / import seluruh data lokal ke satu file JSON. */
public final class BackupManager {
    private BackupManager() {}

    public interface Callback {
        void onDone(boolean ok, String message);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void export(Context ctx, Uri uri, Callback cb) {
        final Context app = ctx.getApplicationContext();
        IO.execute(() -> {
            try {
                String json = toJson(AppDatabase.get(app));
                try (OutputStream os = app.getContentResolver().openOutputStream(uri, "wt")) {
                    if (os == null) throw new IllegalStateException("File tujuan tidak bisa dibuka");
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
                done(cb, true, "Backup berhasil disimpan");
            } catch (Exception e) {
                done(cb, false, "Export gagal: " + e.getMessage());
            }
        });
    }

    public static void restore(Context ctx, Uri uri, Callback cb) {
        final Context app = ctx.getApplicationContext();
        IO.execute(() -> {
            try {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                try (InputStream is = app.getContentResolver().openInputStream(uri)) {
                    if (is == null) throw new IllegalStateException("File tidak bisa dibuka");
                    byte[] chunk = new byte[8192];
                    int n;
                    while ((n = is.read(chunk)) > 0) buf.write(chunk, 0, n);
                }
                fromJson(AppDatabase.get(app), new String(buf.toByteArray(), StandardCharsets.UTF_8));
                done(cb, true, "Data berhasil dipulihkan");
            } catch (Exception e) {
                done(cb, false, "Import gagal: " + e.getMessage());
            }
        });
    }

    /**
     * Salinan pengaman sebelum update aplikasi. Disimpan di penyimpanan internal app
     * (folder backup/, 3 terakhir). Dipanggil di background thread.
     */
    public static File snapshotNow(Context ctx) throws Exception {
        String json = toJson(AppDatabase.get(ctx.getApplicationContext()));
        File dir = new File(ctx.getFilesDir(), "backup");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Folder backup gagal dibuat");
        File f = new File(dir, "pre-update-" + System.currentTimeMillis() + ".json");
        try (java.io.FileOutputStream os = new java.io.FileOutputStream(f)) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
        File[] all = dir.listFiles();
        if (all != null && all.length > 3) {
            java.util.Arrays.sort(all);
            for (int i = 0; i < all.length - 3; i++) all[i].delete();
        }
        return f;
    }

    /** Pulihkan dari snapshot pengaman terbaru (jika update membuat data bermasalah). */
    public static void restoreLatestSnapshot(Context ctx, Callback cb) {
        final Context app = ctx.getApplicationContext();
        IO.execute(() -> {
            try {
                File dir = new File(app.getFilesDir(), "backup");
                File[] all = dir.listFiles();
                if (all == null || all.length == 0) throw new IllegalStateException("Belum ada snapshot");
                java.util.Arrays.sort(all);
                File latest = all[all.length - 1];
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                try (InputStream is = new FileInputStream(latest)) {
                    byte[] chunk = new byte[8192];
                    int n;
                    while ((n = is.read(chunk)) > 0) buf.write(chunk, 0, n);
                }
                fromJson(AppDatabase.get(app), new String(buf.toByteArray(), StandardCharsets.UTF_8));
                done(cb, true, "Snapshot dipulihkan");
            } catch (Exception e) {
                done(cb, false, "Pemulihan snapshot gagal: " + e.getMessage());
            }
        });
    }

    private static void done(Callback cb, boolean ok, String msg) {
        if (cb != null) MAIN.post(() -> cb.onDone(ok, msg));
    }

    // ---------- serialisasi ----------
    private static String toJson(AppDatabase db) throws JSONException {
        JSONObject root = new JSONObject();
        root.put("app", "SaveQuest");
        root.put("version", 4);
        root.put("exportedAt", System.currentTimeMillis());

        PlayerEntity p = db.playerDao().get();
        if (p != null) {
            JSONObject o = new JSONObject();
            o.put("totalExp", p.totalExp);
            o.put("streak", p.streak);
            o.put("longestStreak", p.longestStreak);
            o.put("lastDepositDay", p.lastDepositDay);
            o.put("coins", p.coins);
            o.put("hp", p.hp);
            o.put("avatarId", p.avatarId);
            o.put("frameId", p.frameId);
            o.put("skinId", p.skinId);
            o.put("bgId", p.bgId);
            o.put("lastHpCheckDay", p.lastHpCheckDay);
            o.put("lastSpinDay", p.lastSpinDay);
            o.put("buffMultiplier", p.buffMultiplier);
            o.put("buffUntilDay", p.buffUntilDay);
            o.put("boostMult", p.boostMult);
            o.put("boostCharges", p.boostCharges);
            o.put("boostUntil", p.boostUntil);
            o.put("depositCount", p.depositCount);
            o.put("peakTotal", p.peakTotal);
            o.put("maxDeposit", p.maxDeposit);
            o.put("lifetimeCoins", p.lifetimeCoins);
            o.put("itemsUsed", p.itemsUsed);
            o.put("purchaseCount", p.purchaseCount);
            o.put("spinCount", p.spinCount);
            o.put("questClaims", p.questClaims);
            o.put("shieldSaves", p.shieldSaves);
            o.put("bossKills", p.bossKills);
            o.put("vaultsDone", p.vaultsDone);
            root.put("player", o);
        }

        JSONArray vaults = new JSONArray();
        for (VaultEntity v : db.vaultDao().allSync()) {
            JSONObject o = new JSONObject();
            o.put("id", v.id);
            o.put("name", v.name);
            o.put("target", v.targetAmount);
            o.put("saved", v.savedAmount);
            o.put("lockUntil", v.lockUntil);
            o.put("createdAt", v.createdAt);
            o.put("type", v.type);
            o.put("bossKind", v.bossKind);
            o.put("defeatedAt", v.defeatedAt);
            vaults.put(o);
        }
        root.put("vaults", vaults);

        JSONArray txs = new JSONArray();
        for (TransactionEntity t : db.transactionDao().allSync()) {
            JSONObject o = new JSONObject();
            o.put("id", t.id);
            o.put("vaultId", t.vaultId);
            o.put("amount", t.amount);
            o.put("type", t.type);
            o.put("note", t.note == null ? "" : t.note);
            o.put("exp", t.expEarned);
            o.put("createdAt", t.createdAt);
            o.put("day", t.dayEpoch);
            txs.put(o);
        }
        root.put("transactions", txs);

        JSONArray quests = new JSONArray();
        for (QuestEntity q : db.questDao().allSync()) {
            JSONObject o = new JSONObject();
            o.put("id", q.id);
            o.put("code", q.code);
            o.put("title", q.title);
            o.put("target", q.targetAmount);
            o.put("reward", q.rewardExp);
            o.put("start", q.periodStart);
            o.put("end", q.periodEnd);
            o.put("progress", q.progress);
            o.put("completed", q.completed);
            o.put("claimed", q.claimed);
            quests.put(o);
        }
        root.put("quests", quests);

        JSONArray badges = new JSONArray();
        for (BadgeEntity b : db.badgeDao().allSync()) {
            JSONObject o = new JSONObject();
            o.put("code", b.code);
            o.put("title", b.title);
            o.put("desc", b.description);
            o.put("category", b.category);
            o.put("tier", b.tier);
            o.put("unlockedAt", b.unlockedAt);
            badges.put(o);
        }
        root.put("badges", badges);

        JSONArray owned = new JSONArray();
        for (OwnedItemEntity it : db.ownedItemDao().allSync()) {
            JSONObject o = new JSONObject();
            o.put("itemId", it.itemId);
            o.put("acquiredAt", it.acquiredAt);
            owned.put(o);
        }
        root.put("owned", owned);

        JSONArray inv = new JSONArray();
        for (InventoryEntity it : db.inventoryDao().allSync()) {
            JSONObject o = new JSONObject();
            o.put("itemId", it.itemId);
            o.put("quantity", it.quantity);
            o.put("acquiredAt", it.acquiredAt);
            inv.put(o);
        }
        root.put("inventory", inv);
        return root.toString(2);
    }

    private static void backfillCounters(PlayerEntity p, List<VaultEntity> vaults,
                                          List<TransactionEntity> txs, List<QuestEntity> quests) {
        long total = 0;
        for (VaultEntity v : vaults) {
            total += v.savedAmount;
            if (v.targetAmount > 0 && v.savedAmount >= v.targetAmount) p.vaultsDone++;
            if (VaultEntity.TYPE_BOSS.equals(v.type) && v.defeatedAt > 0) p.bossKills++;
        }
        p.peakTotal = total;
        for (TransactionEntity t : txs) {
            if (t.amount <= 0) continue;
            p.depositCount++;
            p.maxDeposit = Math.max(p.maxDeposit, t.amount);
            p.lifetimeCoins += Math.max(1, t.amount / 1000);
        }
        for (QuestEntity q : quests) if (q.claimed) p.questClaims++;
    }

    private static void fromJson(AppDatabase db, String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        if (!"SaveQuest".equals(root.optString("app"))) {
            throw new JSONException("Bukan file backup SaveQuest");
        }

        // parse semua dulu: kalau file rusak, data lama tidak tersentuh
        PlayerEntity player = null;
        JSONObject po = root.optJSONObject("player");
        if (po != null) {
            player = new PlayerEntity();
            player.totalExp = po.optLong("totalExp");
            player.streak = po.optInt("streak");
            player.longestStreak = po.optInt("longestStreak");
            player.lastDepositDay = po.optLong("lastDepositDay", -1);
            player.coins = po.optLong("coins");
            player.hp = po.optInt("hp", 100);
            String av = po.optString("avatarId", "hero");
            player.avatarId = "custom".equals(av) ? "hero" : av; // foto galeri tidak ikut backup
            player.frameId = po.optString("frameId", "frame_none");
            player.skinId = po.optString("skinId", "skin_default");
            player.bgId = po.optString("bgId", "bg_default");
            player.lastHpCheckDay = po.optLong("lastHpCheckDay", -1);
            player.lastSpinDay = po.optLong("lastSpinDay", -1);
            player.buffMultiplier = po.optDouble("buffMultiplier", 1.0);
            player.buffUntilDay = po.optLong("buffUntilDay", -1);
            player.boostMult = po.optDouble("boostMult", 1.0);
            player.boostCharges = po.optInt("boostCharges");
            player.boostUntil = po.optLong("boostUntil");
            player.depositCount = po.optLong("depositCount");
            player.peakTotal = po.optLong("peakTotal");
            player.maxDeposit = po.optLong("maxDeposit");
            player.lifetimeCoins = po.optLong("lifetimeCoins");
            player.itemsUsed = po.optLong("itemsUsed");
            player.purchaseCount = po.optLong("purchaseCount");
            player.spinCount = po.optLong("spinCount");
            player.questClaims = po.optLong("questClaims");
            player.shieldSaves = po.optLong("shieldSaves");
            player.bossKills = po.optLong("bossKills");
            player.vaultsDone = po.optLong("vaultsDone");
        }

        List<VaultEntity> vaults = new ArrayList<>();
        JSONArray va = root.optJSONArray("vaults");
        for (int i = 0; va != null && i < va.length(); i++) {
            JSONObject o = va.getJSONObject(i);
            VaultEntity v = new VaultEntity();
            v.id = o.getLong("id");
            v.name = o.getString("name");
            v.targetAmount = o.optLong("target");
            v.savedAmount = o.optLong("saved");
            v.lockUntil = o.optLong("lockUntil");
            v.createdAt = o.optLong("createdAt");
            v.type = o.optString("type", VaultEntity.TYPE_NORMAL);
            v.bossKind = o.optInt("bossKind");
            v.defeatedAt = o.optLong("defeatedAt");
            vaults.add(v);
        }

        List<TransactionEntity> txs = new ArrayList<>();
        JSONArray ta = root.optJSONArray("transactions");
        for (int i = 0; ta != null && i < ta.length(); i++) {
            JSONObject o = ta.getJSONObject(i);
            TransactionEntity t = new TransactionEntity();
            t.id = o.getLong("id");
            t.vaultId = o.getLong("vaultId");
            t.amount = o.getLong("amount");
            t.type = o.optString("type", TransactionEntity.DEPOSIT);
            t.note = o.optString("note", "");
            t.expEarned = o.optInt("exp");
            t.createdAt = o.optLong("createdAt");
            t.dayEpoch = o.optLong("day");
            txs.add(t);
        }

        List<QuestEntity> quests = new ArrayList<>();
        JSONArray qa = root.optJSONArray("quests");
        for (int i = 0; qa != null && i < qa.length(); i++) {
            JSONObject o = qa.getJSONObject(i);
            QuestEntity q = new QuestEntity();
            q.id = o.getLong("id");
            q.code = o.getString("code");
            q.title = o.optString("title");
            q.targetAmount = o.optLong("target");
            q.rewardExp = o.optInt("reward");
            q.periodStart = o.optLong("start");
            q.periodEnd = o.optLong("end");
            q.progress = o.optLong("progress");
            q.completed = o.optBoolean("completed");
            q.claimed = o.optBoolean("claimed", q.completed);
            quests.add(q);
        }

        List<BadgeEntity> badges = new ArrayList<>();
        JSONArray ba = root.optJSONArray("badges");
        for (int i = 0; ba != null && i < ba.length(); i++) {
            JSONObject o = ba.getJSONObject(i);
            BadgeEntity b = new BadgeEntity();
            b.code = o.getString("code");
            b.title = o.optString("title");
            b.description = o.optString("desc");
            b.category = o.optString("category");
            b.tier = o.optInt("tier", 1);
            b.unlockedAt = o.optLong("unlockedAt");
            badges.add(b);
        }

        List<OwnedItemEntity> owned = new ArrayList<>();
        JSONArray oa = root.optJSONArray("owned");
        for (int i = 0; oa != null && i < oa.length(); i++) {
            JSONObject o = oa.getJSONObject(i);
            OwnedItemEntity it = new OwnedItemEntity();
            it.itemId = o.getString("itemId");
            it.acquiredAt = o.optLong("acquiredAt");
            owned.add(it);
        }

        // backup dari versi lama tidak punya penghitung badge: hitung ulang dari riwayat
        if (player == null) player = new PlayerEntity();
        if (po == null || !po.has("depositCount")) backfillCounters(player, vaults, txs, quests);

        List<InventoryEntity> inventory = new ArrayList<>();
        JSONArray ia = root.optJSONArray("inventory");
        for (int i = 0; ia != null && i < ia.length(); i++) {
            JSONObject o = ia.getJSONObject(i);
            InventoryEntity it = new InventoryEntity();
            it.itemId = o.getString("itemId");
            it.quantity = o.optInt("quantity");
            it.acquiredAt = o.optLong("acquiredAt");
            inventory.add(it);
        }

        final PlayerEntity fp = player;
        db.runInTransaction(() -> {
            db.transactionDao().deleteAll();
            db.questDao().deleteAll();
            db.vaultDao().deleteAll();
            db.badgeDao().deleteAll();
            db.ownedItemDao().deleteAll();
            db.inventoryDao().deleteAll();
            db.playerDao().deleteAll();
            if (fp != null) db.playerDao().replace(fp); else db.playerDao().replace(new PlayerEntity());
            db.vaultDao().insertAllReplace(vaults);
            db.transactionDao().insertAllReplace(txs);
            db.questDao().insertAllReplace(quests);
            db.badgeDao().insertAllReplace(badges);
            db.ownedItemDao().insertAllReplace(owned);
            db.inventoryDao().insertAllReplace(inventory);
        });
    }
}
