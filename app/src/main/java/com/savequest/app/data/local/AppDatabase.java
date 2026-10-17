package com.savequest.app.data.local;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.savequest.app.data.local.dao.BadgeDao;
import com.savequest.app.data.local.dao.InventoryDao;
import com.savequest.app.data.local.dao.OwnedItemDao;
import com.savequest.app.data.local.dao.PlayerDao;
import com.savequest.app.data.local.dao.QuestDao;
import com.savequest.app.data.local.dao.ShopItemDao;
import com.savequest.app.data.local.dao.TransactionDao;
import com.savequest.app.data.local.dao.VaultDao;
import com.savequest.app.data.local.entity.BadgeEntity;
import com.savequest.app.data.local.entity.InventoryEntity;
import com.savequest.app.data.local.entity.OwnedItemEntity;
import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.data.local.entity.QuestEntity;
import com.savequest.app.data.local.entity.ShopItemEntity;
import com.savequest.app.data.local.entity.TransactionEntity;
import com.savequest.app.data.local.entity.VaultEntity;

/**
 * Seluruh data (vault, level, EXP, streak, inventory, badge) tersimpan di sini.
 * Database TIDAK pernah dihapus saat update: setiap perubahan skema memakai Migration,
 * dan tidak ada fallbackToDestructiveMigration.
 */
@Database(entities = {PlayerEntity.class, VaultEntity.class, TransactionEntity.class,
        QuestEntity.class, BadgeEntity.class, OwnedItemEntity.class,
        InventoryEntity.class, ShopItemEntity.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract PlayerDao playerDao();
    public abstract VaultDao vaultDao();
    public abstract TransactionDao transactionDao();
    public abstract QuestDao questDao();
    public abstract BadgeDao badgeDao();
    public abstract OwnedItemDao ownedItemDao();
    public abstract InventoryDao inventoryDao();
    public abstract ShopItemDao shopItemDao();

    /** v1 -> v2: sistem klaim quest + kategori badge. */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE quests ADD COLUMN claimed INTEGER NOT NULL DEFAULT 0");
            db.execSQL("UPDATE quests SET claimed = completed");
            db.execSQL("ALTER TABLE badges ADD COLUMN category TEXT");
        }
    };

    /** v2 -> v3 (2.1): koin, HP, avatar, kosmetik, boss/darurat, buff. */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE player ADD COLUMN coins INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE player ADD COLUMN hp INTEGER NOT NULL DEFAULT 100");
            db.execSQL("ALTER TABLE player ADD COLUMN avatarId TEXT DEFAULT 'hero'");
            db.execSQL("ALTER TABLE player ADD COLUMN frameId TEXT DEFAULT 'frame_none'");
            db.execSQL("ALTER TABLE player ADD COLUMN skinId TEXT DEFAULT 'skin_default'");
            db.execSQL("ALTER TABLE player ADD COLUMN bgId TEXT DEFAULT 'bg_default'");
            db.execSQL("ALTER TABLE player ADD COLUMN customAvatarPath TEXT");
            db.execSQL("ALTER TABLE player ADD COLUMN lastHpCheckDay INTEGER NOT NULL DEFAULT -1");
            db.execSQL("ALTER TABLE player ADD COLUMN lastSpinDay INTEGER NOT NULL DEFAULT -1");
            db.execSQL("ALTER TABLE player ADD COLUMN buffMultiplier REAL NOT NULL DEFAULT 1.0");
            db.execSQL("ALTER TABLE player ADD COLUMN buffUntilDay INTEGER NOT NULL DEFAULT -1");

            db.execSQL("ALTER TABLE vaults ADD COLUMN type TEXT DEFAULT 'NORMAL'");
            db.execSQL("ALTER TABLE vaults ADD COLUMN bossKind INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE vaults ADD COLUMN defeatedAt INTEGER NOT NULL DEFAULT 0");

            db.execSQL("CREATE TABLE IF NOT EXISTS owned_items (itemId TEXT NOT NULL, acquiredAt INTEGER NOT NULL, PRIMARY KEY(itemId))");
        }
    };

    /**
     * v3 -> v4 (2.2): inventory, katalog toko, booster, penghitung badge, tier badge.
     * Penghitung diisi ulang dari riwayat transaksi lama supaya badge pengguna lama akurat.
     */
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE player ADD COLUMN boostMult REAL NOT NULL DEFAULT 1.0");
            db.execSQL("ALTER TABLE player ADD COLUMN boostCharges INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE player ADD COLUMN boostUntil INTEGER NOT NULL DEFAULT 0");
            String[] counters = {"depositCount", "peakTotal", "maxDeposit", "lifetimeCoins", "itemsUsed",
                    "purchaseCount", "spinCount", "questClaims", "shieldSaves", "bossKills", "vaultsDone"};
            for (String c : counters) {
                db.execSQL("ALTER TABLE player ADD COLUMN " + c + " INTEGER NOT NULL DEFAULT 0");
            }
            db.execSQL("UPDATE player SET "
                    + "depositCount = (SELECT COUNT(*) FROM transactions WHERE amount > 0), "
                    + "peakTotal = (SELECT COALESCE(SUM(savedAmount), 0) FROM vaults), "
                    + "maxDeposit = (SELECT COALESCE(MAX(amount), 0) FROM transactions), "
                    + "lifetimeCoins = (SELECT COALESCE(SUM(CASE WHEN amount / 1000 < 1 THEN 1 ELSE amount / 1000 END), 0) FROM transactions WHERE amount > 0), "
                    + "bossKills = (SELECT COUNT(*) FROM vaults WHERE type = 'BOSS' AND defeatedAt > 0), "
                    + "vaultsDone = (SELECT COUNT(*) FROM vaults WHERE targetAmount > 0 AND savedAmount >= targetAmount), "
                    + "questClaims = (SELECT COUNT(*) FROM quests WHERE claimed = 1)");

            db.execSQL("ALTER TABLE badges ADD COLUMN tier INTEGER NOT NULL DEFAULT 1");

            db.execSQL("CREATE TABLE IF NOT EXISTS inventory (itemId TEXT NOT NULL, quantity INTEGER NOT NULL, acquiredAt INTEGER NOT NULL, PRIMARY KEY(itemId))");
            db.execSQL("CREATE TABLE IF NOT EXISTS shop_items (id TEXT NOT NULL, category TEXT, name TEXT, description TEXT, "
                    + "price INTEGER NOT NULL, effectType TEXT, effectValue REAL NOT NULL, effectExtra REAL NOT NULL, "
                    + "rarity INTEGER NOT NULL, iconKey TEXT, sortOrder INTEGER NOT NULL, enabled INTEGER NOT NULL, PRIMARY KEY(id))");
        }
    };

    public static AppDatabase get(Context ctx) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(ctx.getApplicationContext(),
                                    AppDatabase.class, "savequest.db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                            .build();
                }
            }
        }
        return instance;
    }
}
