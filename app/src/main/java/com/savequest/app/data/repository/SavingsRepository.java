package com.savequest.app.data.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import com.savequest.app.data.local.AppDatabase;
import com.savequest.app.data.local.entity.BadgeEntity;
import com.savequest.app.data.local.entity.DayTotal;
import com.savequest.app.data.local.entity.InventoryEntity;
import com.savequest.app.data.local.entity.OwnedItemEntity;
import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.data.local.entity.QuestEntity;
import com.savequest.app.data.local.entity.ShopItemEntity;
import com.savequest.app.data.local.entity.TransactionEntity;
import com.savequest.app.data.local.entity.TxWithVault;
import com.savequest.app.data.local.entity.VaultEntity;
import com.savequest.app.domain.AvatarCatalog;
import com.savequest.app.domain.BadgeCatalog;
import com.savequest.app.domain.BadgeEngine;
import com.savequest.app.domain.BossSystem;
import com.savequest.app.domain.CosmeticCatalog;
import com.savequest.app.domain.EconomySystem;
import com.savequest.app.domain.ExpCalculator;
import com.savequest.app.domain.HpSystem;
import com.savequest.app.domain.ItemCatalog;
import com.savequest.app.domain.ItemEffectManager;
import com.savequest.app.domain.LevelSystem;
import com.savequest.app.domain.PenaltySystem;
import com.savequest.app.domain.QuestCatalog;
import com.savequest.app.domain.SpinSystem;
import com.savequest.app.domain.StreakSystem;
import com.savequest.app.util.DateUtil;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Satu-satunya pintu ke database. Logika game ada di sini + package domain. */
public class SavingsRepository {
    public interface Callback {
        void onResult(SaveResult result);
    }

    private final AppDatabase db;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Random rng = new Random();

    public SavingsRepository(Context ctx) {
        db = AppDatabase.get(ctx);
        io.execute(this::seed);
    }

    // ---------- observasi ----------
    public LiveData<PlayerEntity> player() { return db.playerDao().observe(); }
    public LiveData<List<VaultEntity>> vaults() { return db.vaultDao().observeAll(); }
    public LiveData<Long> totalSaved() { return db.vaultDao().observeTotal(); }
    public LiveData<Long> depositSum(long from, long to) { return db.transactionDao().observeDepositSum(from, to); }
    public LiveData<List<QuestEntity>> activeQuests(long day) { return db.questDao().observeActive(day); }
    public LiveData<List<BadgeEntity>> badges() { return db.badgeDao().observeAll(); }
    public LiveData<List<TxWithVault>> recent(int limit) { return db.transactionDao().observeRecent(limit); }
    public LiveData<List<DayTotal>> dailyTotals(long from, long to) { return db.transactionDao().observeDailyTotals(from, to); }
    public LiveData<List<OwnedItemEntity>> ownedItems() { return db.ownedItemDao().observeAll(); }
    public LiveData<List<InventoryEntity>> inventory() { return db.inventoryDao().observeAll(); }
    public LiveData<List<ShopItemEntity>> shopItems() { return db.shopItemDao().observeEnabled(); }

    // ---------- seed & refresh ----------
    private void seed() {
        db.playerDao().insertIgnore(new PlayerEntity());
        db.badgeDao().insertAll(BadgeCatalog.entities());
        for (BadgeCatalog.Def d : BadgeCatalog.ALL) {
            db.badgeDao().updateMeta(d.code, d.title, d.description, d.category, d.tier);
        }
        db.shopItemDao().insertAllReplace(ItemCatalog.defaults());
        ensureQuests();
    }

    /**
     * Dipanggil saat app dibuka: sinkron quest, denda HP hari terlewat, dan cek badge
     * (termasuk badge baru untuk pengguna lama setelah update).
     */
    public void refreshAsync(Callback cb) {
        io.execute(() -> {
            ensureQuests();
            SaveResult missed = applyMissedDays();
            final SaveResult r = missed != null ? missed : new SaveResult();
            db.runInTransaction(() -> {
                PlayerEntity p = db.playerDao().get();
                if (p != null) evaluateBadges(p, r);
            });
            if (missed != null || !r.badges.isEmpty()) {
                if (SaveResult.OTHER.equals(r.kind)) r.kind = SaveResult.BADGE;
                post(cb, r);
            }
        });
    }

    private void ensureQuests() {
        long today = DateUtil.today();
        long weekStart = DateUtil.weekStart(today);
        for (QuestCatalog.Template t : QuestCatalog.ALL) {
            QuestEntity q = new QuestEntity();
            q.code = t.code;
            q.title = t.title;
            q.targetAmount = t.target;
            q.rewardExp = t.rewardExp;
            q.periodStart = t.weekly ? weekStart : today;
            q.periodEnd = t.weekly ? weekStart + 6 : today;
            db.questDao().insertIgnore(q);
        }
    }

    /** Denda HP: tiap hari menabung yang terlewat sejak pengecekan terakhir. */
    private SaveResult applyMissedDays() {
        final SaveResult[] out = new SaveResult[1];
        db.runInTransaction(() -> {
            PlayerEntity p = db.playerDao().get();
            if (p == null) return;
            long yesterday = DateUtil.today() - 1;
            if (p.lastHpCheckDay < 0) {
                p.lastHpCheckDay = yesterday;
                db.playerDao().update(p);
                return;
            }
            if (p.lastHpCheckDay >= yesterday) return;

            long from = Math.max(p.lastHpCheckDay + 1, yesterday - 13);
            Long firstDay = db.transactionDao().minDepositDaySync();
            p.lastHpCheckDay = yesterday;
            if (firstDay != null) {
                from = Math.max(from, firstDay);
                List<Long> days = db.transactionDao().depositDaysSync(from, yesterday);
                int missed = 0;
                for (long d = from; d <= yesterday; d++) if (!days.contains(d)) missed++;
                if (missed > 0) {
                    int before = p.hp;
                    p.hp = Math.max(0, p.hp - HpSystem.missedPenalty(missed));
                    SaveResult r = new SaveResult();
                    r.kind = SaveResult.HP;
                    r.hpDelta = p.hp - before;
                    r.info = "Kamu melewatkan " + missed + " hari menabung. HP " + r.hpDelta;
                    out[0] = r;
                }
            }
            db.playerDao().update(p);
        });
        return out[0];
    }

    // ---------- vault ----------
    public void createVault(String name, long target, int lockDays, String type, int bossKind, Callback cb) {
        io.execute(() -> {
            SaveResult r = new SaveResult();
            if (name.isEmpty() || target <= 0) {
                r.error = "Nama dan target wajib diisi";
            } else {
                VaultEntity v = new VaultEntity();
                v.name = name;
                v.targetAmount = target;
                v.createdAt = System.currentTimeMillis();
                v.type = type;
                v.bossKind = bossKind;
                int days = lockDays;
                if (VaultEntity.TYPE_EMERGENCY.equals(type) && days <= 0) days = PenaltySystem.EMERGENCY_DEFAULT_LOCK_DAYS;
                v.lockUntil = days > 0 ? v.createdAt + days * 86_400_000L : 0;
                db.vaultDao().insert(v);
                r.info = VaultEntity.TYPE_BOSS.equals(type)
                        ? "Boss \"" + name + "\" muncul! Kalahkan dengan menabung."
                        : "Vault \"" + name + "\" dibuat";
            }
            post(cb, r);
        });
    }

    public void deleteVault(VaultEntity v, Callback cb) {
        io.execute(() -> {
            SaveResult r = new SaveResult();
            if (v.lockUntil > System.currentTimeMillis()) {
                r.error = "Vault terkunci sampai " + DateUtil.formatDate(v.lockUntil);
            } else {
                db.vaultDao().delete(v);
                r.info = "Vault dihapus";
            }
            post(cb, r);
        });
    }

    // ---------- transaksi ----------
    public void deposit(long vaultId, long amount, String type, String note, Callback cb) {
        io.execute(() -> {
            SaveResult r = new SaveResult();
            r.kind = SaveResult.DEPOSIT;
            if (amount <= 0) {
                r.error = "Nominal harus lebih dari 0";
            } else if (db.vaultDao().getById(vaultId) == null) {
                r.error = "Vault tidak ditemukan";
            } else {
                ensureQuests();
                db.runInTransaction(() -> doDeposit(vaultId, amount, type, note, r));
            }
            post(cb, r);
        });
    }

    /**
     * Tarik tabungan. Vault biasa yang terkunci = ditolak. Emergency Vault yang terkunci hanya
     * bisa ditarik dengan force = true (user sudah melihat konfirmasi) dan kena penalti EXP + HP.
     */
    public void withdraw(long vaultId, long amount, String note, boolean force, Callback cb) {
        io.execute(() -> {
            SaveResult r = new SaveResult();
            r.kind = SaveResult.WITHDRAW;
            db.runInTransaction(() -> {
                VaultEntity v = db.vaultDao().getById(vaultId);
                if (v == null) {
                    r.error = "Vault tidak ditemukan";
                    return;
                }
                boolean locked = v.lockUntil > System.currentTimeMillis();
                boolean emergency = VaultEntity.TYPE_EMERGENCY.equals(v.type);
                if (locked && !(emergency && force)) {
                    r.error = "Vault terkunci sampai " + DateUtil.formatDate(v.lockUntil);
                    return;
                }
                if (amount <= 0 || amount > v.savedAmount) {
                    r.error = "Nominal tidak valid atau saldo kurang";
                    return;
                }
                PlayerEntity p = db.playerDao().get();
                int hpLoss = HpSystem.withdrawPenalty(amount, v.savedAmount);
                int expLoss = 0;
                if (locked && emergency) {
                    expLoss = PenaltySystem.emergencyExp(amount);
                    hpLoss += PenaltySystem.EMERGENCY_HP;
                }
                db.transactionDao().insert(newTx(vaultId, -amount, TransactionEntity.WITHDRAW,
                        locked && emergency ? "Penarikan darurat. " + (note == null ? "" : note) : note, 0));
                db.vaultDao().addSaved(vaultId, -amount);

                int hpBefore = p.hp;
                p.hp = Math.max(0, p.hp - hpLoss);
                p.totalExp = Math.max(0, p.totalExp - expLoss);
                db.playerDao().update(p);
                r.hpDelta = p.hp - hpBefore;
                r.expLost = expLoss;
                StringBuilder s = new StringBuilder("Penarikan berhasil");
                if (expLoss > 0) s.append(". Penalti -").append(expLoss).append(" EXP");
                if (r.hpDelta < 0) s.append(", HP ").append(r.hpDelta);
                r.info = s.toString();
            });
            post(cb, r);
        });
    }

    /** Klaim hadiah EXP + koin + item dari quest yang sudah selesai. */
    public void claimQuest(long questId, Callback cb) {
        io.execute(() -> {
            SaveResult r = new SaveResult();
            r.kind = SaveResult.CLAIM;
            db.runInTransaction(() -> {
                QuestEntity q = db.questDao().getById(questId);
                if (q == null || !q.completed || q.claimed) {
                    r.error = "Quest ini tidak bisa diklaim";
                    return;
                }
                PlayerEntity p = db.playerDao().get();
                int oldLevel = LevelSystem.levelFor(p.totalExp);
                q.claimed = true;
                db.questDao().update(q);
                p.totalExp += q.rewardExp;
                r.expGained = q.rewardExp;
                r.coinsGained = EconomySystem.questCoins(q.rewardExp);
                p.coins += r.coinsGained;
                p.lifetimeCoins += r.coinsGained;
                p.questClaims++;
                int hpBefore = p.hp;
                p.hp = Math.min(HpSystem.MAX, p.hp + HpSystem.healOnClaim());
                r.hpDelta = p.hp - hpBefore;

                String item = QuestCatalog.rewardItemFor(q.code);
                if (item != null) {
                    grant(item, 1);
                    r.itemsGranted.add(itemName(item) + " x1");
                }
                r.info = q.title;
                int newLevel = LevelSystem.levelFor(p.totalExp);
                r.leveledUp = newLevel > oldLevel;
                r.newLevel = newLevel;
                evaluateBadges(p, r);
                db.playerDao().update(p);
            });
            post(cb, r);
        });
    }

    private void doDeposit(long vaultId, long amount, String type, String note, SaveResult r) {
        long today = DateUtil.today();
        long now = System.currentTimeMillis();
        PlayerEntity p = db.playerDao().get();
        int oldLevel = LevelSystem.levelFor(p.totalExp);

        // streak, dengan penyelamatan otomatis oleh Perisai / Gulungan jika ada hari terlewat
        if (p.lastDepositDay != today) {
            int gap = StreakSystem.gapDays(p.lastDepositDay, today);
            boolean continues = gap == 0;
            if (gap >= 1) {
                String item = ItemEffectManager.pickProtection(gap,
                        db.inventoryDao().quantityOf(ItemCatalog.STREAK_SHIELD),
                        db.inventoryDao().quantityOf(ItemCatalog.PROTECT_SCROLL));
                if (item != null && db.inventoryDao().consumeOne(item) > 0) {
                    db.inventoryDao().deleteEmpty();
                    continues = true;
                    r.shieldUsed = item;
                    p.shieldSaves++;
                }
            }
            p.streak = continues ? p.streak + 1 : 1;
            p.longestStreak = Math.max(p.longestStreak, p.streak);
            p.lastDepositDay = today;
        }

        // EXP = base x streak x (buff roda atau booster item, yang terbesar) x faktor HP
        double streakMult = StreakSystem.multiplier(p.streak);
        double spinBuff = today <= p.buffUntilDay ? p.buffMultiplier : 1.0;
        double boost = ItemEffectManager.boostMultiplier(p, now);
        double buff = Math.max(spinBuff, boost);
        double hpMult = HpSystem.expMultiplier(p.hp);
        r.baseExp = ExpCalculator.base(amount);
        r.multiplier = streakMult;
        r.buff = buff;
        int exp = ExpCalculator.compute(amount, streakMult, buff, hpMult);
        if (p.boostCharges > 0) {
            p.boostCharges--;
            if (p.boostCharges == 0 && now >= p.boostUntil) p.boostMult = 1.0;
        }

        db.transactionDao().insert(newTx(vaultId, amount, type, note, exp));
        db.vaultDao().addSaved(vaultId, amount);
        p.totalExp += exp;
        r.expGained = exp;

        // koin, HP, dan penghitung statistik
        r.coinsGained = EconomySystem.coinsForDeposit(amount);
        p.coins += r.coinsGained;
        p.lifetimeCoins += r.coinsGained;
        int hpBefore = p.hp;
        p.hp = Math.min(HpSystem.MAX, p.hp + HpSystem.healOnDeposit());
        r.hpDelta = p.hp - hpBefore;
        p.depositCount++;
        p.maxDeposit = Math.max(p.maxDeposit, amount);
        p.peakTotal = Math.max(p.peakTotal, db.vaultDao().totalSync());
        p.vaultsDone = Math.max(p.vaultsDone, db.vaultDao().completedCount());

        // boss kalah / target tercapai pertama kali
        VaultEntity v = db.vaultDao().getById(vaultId);
        if (v != null && v.defeatedAt == 0 && v.targetAmount > 0 && v.savedAmount >= v.targetAmount) {
            v.defeatedAt = System.currentTimeMillis();
            db.vaultDao().update(v);
            if (VaultEntity.TYPE_BOSS.equals(v.type)) {
                int bonusExp = BossSystem.defeatExp(v.targetAmount);
                long bonusCoins = BossSystem.defeatCoins(v.targetAmount);
                p.totalExp += bonusExp;
                p.coins += bonusCoins;
                p.lifetimeCoins += bonusCoins;
                p.bossKills++;
                r.expGained += bonusExp;
                r.coinsGained += bonusCoins;
                r.bossesDefeated.add(v.name + "\u001F" + bonusExp + "\u001F" + bonusCoins);
                grant(ItemCatalog.ELIXIR_HP, 1);
                r.itemsGranted.add(itemName(ItemCatalog.ELIXIR_HP) + " x1");
            }
        }

        // quest: tandai selesai, hadiah diklaim manual
        for (QuestEntity q : db.questDao().activeSync(today)) {
            q.progress = db.transactionDao().depositSumSync(q.periodStart, q.periodEnd);
            if (!q.completed && q.progress >= q.targetAmount) {
                q.completed = true;
                r.questsReady.add(q);
            }
            db.questDao().update(q);
        }

        int newLevel = LevelSystem.levelFor(p.totalExp);
        r.leveledUp = newLevel > oldLevel;
        r.newLevel = newLevel;
        evaluateBadges(p, r, BadgeEngine.EVENT_DEPOSIT);
        db.playerDao().update(p);
    }

    // ---------- Daily Spin ----------
    /** Putar roda: 1x sehari, hanya setelah ada setoran hari ini. Hadiah langsung diterapkan. */
    public void spin(Callback cb) {
        io.execute(() -> {
            SaveResult r = new SaveResult();
            r.kind = SaveResult.SPIN;
            db.runInTransaction(() -> {
                long today = DateUtil.today();
                PlayerEntity p = db.playerDao().get();
                if (p.lastSpinDay == today) {
                    r.error = "Kamu sudah memutar roda hari ini. Kembali lagi besok!";
                    return;
                }
                if (db.transactionDao().depositSumSync(today, today) <= 0) {
                    r.error = "Setor dulu hari ini untuk membuka roda keberuntungan";
                    return;
                }
                int oldLevel = LevelSystem.levelFor(p.totalExp);
                int idx = SpinSystem.pick(rng);
                SpinSystem.Prize prize = SpinSystem.PRIZES[idx];
                r.spinIndex = idx;
                switch (prize.type) {
                    case SpinSystem.EXP:
                        p.totalExp += (long) prize.value;
                        r.expGained = (int) prize.value;
                        break;
                    case SpinSystem.COINS:
                        p.coins += (long) prize.value;
                        p.lifetimeCoins += (long) prize.value;
                        r.coinsGained = (long) prize.value;
                        break;
                    case SpinSystem.HEAL:
                        int before = p.hp;
                        p.hp = Math.min(HpSystem.MAX, p.hp + (int) prize.value);
                        r.hpDelta = p.hp - before;
                        break;
                    case SpinSystem.ITEM:
                        grant(prize.itemId, (int) prize.value);
                        r.itemsGranted.add(itemName(prize.itemId) + " x" + (int) prize.value);
                        break;
                    default:
                        boolean active = today <= p.buffUntilDay;
                        if (!active || prize.value >= p.buffMultiplier) {
                            p.buffMultiplier = prize.value;
                            p.buffUntilDay = today + prize.days;
                        }
                        r.buff = prize.value;
                }
                p.lastSpinDay = today;
                p.spinCount++;
                int newLevel = LevelSystem.levelFor(p.totalExp);
                r.leveledUp = newLevel > oldLevel;
                r.newLevel = newLevel;
                evaluateBadges(p, r);
                db.playerDao().update(p);
                r.info = "Hadiah roda: " + prize.label;
            });
            post(cb, r);
        });
    }

    // ---------- Toko item (consumable) ----------
    /** Beli item habis-pakai sebanyak qty. Koin dipotong, item masuk ke Ransel. */
    public void buyShopItem(String itemId, int qty, Callback cb) {
        io.execute(() -> {
            SaveResult r = new SaveResult();
            r.kind = SaveResult.SHOP;
            db.runInTransaction(() -> {
                ShopItemEntity it = db.shopItemDao().get(itemId);
                if (it == null) it = ItemCatalog.find(itemId);
                int n = Math.max(1, Math.min(99, qty));
                PlayerEntity p = db.playerDao().get();
                if (it == null || !it.enabled) {
                    r.error = "Item tidak tersedia";
                    return;
                }
                long total = (long) it.price * n;
                if (p.coins < total) {
                    r.error = "Koin kurang (" + p.coins + " / " + total + ")";
                    return;
                }
                p.coins -= total;
                p.purchaseCount += n;
                grant(itemId, n);
                r.itemsGranted.add(it.name + " x" + n);
                r.info = "Membeli " + n + "x " + it.name + " (-" + total + " koin)";
                evaluateBadges(p, r);
                db.playerDao().update(p);
            });
            post(cb, r);
        });
    }

    /** Gunakan 1 item dari Ransel. Efek dihitung ItemEffectManager lalu diterapkan secara atomik. */
    public void useItem(String itemId, Callback cb) {
        io.execute(() -> {
            SaveResult r = new SaveResult();
            r.kind = SaveResult.ITEM;
            db.runInTransaction(() -> {
                ShopItemEntity it = ItemCatalog.find(itemId);
                if (it == null) {
                    r.error = "Item tidak dikenal";
                    return;
                }
                if (db.inventoryDao().quantityOf(itemId) <= 0) {
                    r.error = it.name + " sudah habis";
                    return;
                }
                PlayerEntity p = db.playerDao().get();
                int oldLevel = LevelSystem.levelFor(p.totalExp);
                Set<String> owned = new HashSet<>();
                for (OwnedItemEntity o : db.ownedItemDao().allSync()) owned.add(o.itemId);

                ItemEffectManager.Outcome o = ItemEffectManager.apply(it, p, rng,
                        System.currentTimeMillis(), DateUtil.today(), owned);
                if (o.error != null) {
                    r.error = o.error;
                    return;
                }
                if (o.cosmeticId != null) {
                    OwnedItemEntity ow = new OwnedItemEntity();
                    ow.itemId = o.cosmeticId;
                    ow.acquiredAt = System.currentTimeMillis();
                    db.ownedItemDao().insert(ow);
                }
                if (o.consume) {
                    db.inventoryDao().consumeOne(itemId);
                    db.inventoryDao().deleteEmpty();
                }
                p.itemsUsed++;
                r.hpDelta = o.hpDelta;
                r.expGained = o.exp;
                r.coinsGained = o.coins;
                r.info = o.message;
                int newLevel = LevelSystem.levelFor(p.totalExp);
                r.leveledUp = newLevel > oldLevel;
                r.newLevel = newLevel;
                if (o.healedFromCritical) evaluateBadges(p, r, BadgeEngine.EVENT_HEAL_CRITICAL);
                else evaluateBadges(p, r);
                db.playerDao().update(p);
            });
            post(cb, r);
        });
    }

    // ---------- Toko kosmetik & Avatar ----------
    public void buyItem(String itemId, Callback cb) {
        io.execute(() -> {
            SaveResult r = new SaveResult();
            r.kind = SaveResult.SHOP;
            db.runInTransaction(() -> {
                CosmeticCatalog.Item it = CosmeticCatalog.find(itemId);
                PlayerEntity p = db.playerDao().get();
                if (it == null) {
                    r.error = "Item tidak ditemukan";
                } else if (it.isFree() || db.ownedItemDao().get(itemId) != null) {
                    r.error = "Item ini sudah kamu miliki";
                } else if (p.coins < it.price) {
                    r.error = "Koin kurang (" + p.coins + " / " + it.price + ")";
                } else {
                    p.coins -= it.price;
                    p.purchaseCount++;
                    OwnedItemEntity o = new OwnedItemEntity();
                    o.itemId = itemId;
                    o.acquiredAt = System.currentTimeMillis();
                    db.ownedItemDao().insert(o);
                    r.info = "Berhasil membeli " + it.name;
                    evaluateBadges(p, r);
                    db.playerDao().update(p);
                }
            });
            post(cb, r);
        });
    }

    public void equipItem(String itemId, Callback cb) {
        io.execute(() -> {
            SaveResult r = new SaveResult();
            r.kind = SaveResult.SHOP;
            db.runInTransaction(() -> {
                CosmeticCatalog.Item it = CosmeticCatalog.find(itemId);
                if (it == null || !(it.isFree() || db.ownedItemDao().get(itemId) != null)) {
                    r.error = "Beli item ini dulu di Toko";
                    return;
                }
                PlayerEntity p = db.playerDao().get();
                if (CosmeticCatalog.FRAME.equals(it.category)) p.frameId = it.id;
                else if (CosmeticCatalog.SKIN.equals(it.category)) p.skinId = it.id;
                else p.bgId = it.id;
                db.playerDao().update(p);
                r.info = it.name + " dipakai";
            });
            post(cb, r);
        });
    }

    public void setAvatar(String avatarId, Callback cb) {
        io.execute(() -> {
            SaveResult r = new SaveResult();
            r.kind = SaveResult.SHOP;
            db.runInTransaction(() -> {
                PlayerEntity p = db.playerDao().get();
                int level = LevelSystem.levelFor(p.totalExp);
                AvatarCatalog.Def d = AvatarCatalog.find(avatarId);
                if (AvatarCatalog.CUSTOM.equals(avatarId)) {
                    if (p.customAvatarPath == null) {
                        r.error = "Belum ada foto. Unggah dari galeri dulu.";
                        return;
                    }
                } else if (d == null || level < d.minLevel) {
                    r.error = "Avatar ini terbuka di Level " + (d == null ? "?" : d.minLevel);
                    return;
                }
                p.avatarId = avatarId;
                db.playerDao().update(p);
                r.info = "Avatar diganti";
            });
            post(cb, r);
        });
    }

    public void setCustomAvatar(String path, Callback cb) {
        io.execute(() -> {
            SaveResult r = new SaveResult();
            r.kind = SaveResult.SHOP;
            PlayerEntity p = db.playerDao().get();
            p.customAvatarPath = path;
            p.avatarId = AvatarCatalog.CUSTOM;
            db.playerDao().update(p);
            r.info = "Foto avatar diperbarui";
            post(cb, r);
        });
    }

    // ---------- inventory & badge ----------
    private void grant(String itemId, int qty) {
        InventoryEntity e = new InventoryEntity();
        e.itemId = itemId;
        e.quantity = 0;
        e.acquiredAt = System.currentTimeMillis();
        db.inventoryDao().insertIgnore(e);
        db.inventoryDao().increase(itemId, qty);
    }

    private static String itemName(String itemId) {
        ShopItemEntity it = ItemCatalog.find(itemId);
        return it == null ? itemId : it.name;
    }

    /** Buka semua badge yang syaratnya baru terpenuhi. Pemanggil bertanggung jawab menyimpan player. */
    private void evaluateBadges(PlayerEntity p, SaveResult r, String... events) {
        Set<String> ev = new HashSet<>(Arrays.asList(events));
        Set<String> have = new HashSet<>();
        for (BadgeEntity b : db.badgeDao().unlockedSync()) have.add(b.code);
        long now = System.currentTimeMillis();
        for (BadgeCatalog.Def d : BadgeEngine.newlyUnlocked(p, have, Calendar.getInstance(), ev)) {
            BadgeEntity b = db.badgeDao().get(d.code);
            if (b == null) {
                b = new BadgeEntity();
                b.code = d.code;
                b.title = d.title;
                b.description = d.description;
                b.category = d.category;
                b.tier = d.tier;
                b.unlockedAt = now;
                db.badgeDao().insertAllReplace(Collections.singletonList(b));
            } else {
                b.unlockedAt = now;
                db.badgeDao().update(b);
            }
            r.badges.add(d);
        }
    }

    private TransactionEntity newTx(long vaultId, long amount, String type, String note, int exp) {
        TransactionEntity t = new TransactionEntity();
        t.vaultId = vaultId;
        t.amount = amount;
        t.type = type;
        t.note = note == null ? "" : note;
        t.expEarned = exp;
        t.createdAt = System.currentTimeMillis();
        t.dayEpoch = DateUtil.today();
        return t;
    }

    private void post(Callback cb, SaveResult r) {
        if (cb != null) main.post(() -> cb.onResult(r));
    }
}
