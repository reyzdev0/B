package com.savequest.app.ui.main;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.savequest.app.data.local.entity.BadgeEntity;
import com.savequest.app.data.local.entity.DayTotal;
import com.savequest.app.data.local.entity.InventoryEntity;
import com.savequest.app.data.local.entity.ShopItemEntity;
import com.savequest.app.data.local.entity.OwnedItemEntity;
import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.data.local.entity.QuestEntity;
import com.savequest.app.data.local.entity.TransactionEntity;
import com.savequest.app.data.local.entity.TxWithVault;
import com.savequest.app.data.local.entity.VaultEntity;
import com.savequest.app.data.repository.SaveResult;
import com.savequest.app.data.repository.SavingsRepository;
import com.savequest.app.domain.RoundUp;
import com.savequest.app.util.DateUtil;
import com.savequest.app.util.Money;

import java.util.List;

/** ViewModel bersama (scope Activity) untuk Home, Badge, Settings, Avatar, dan Toko. */
public class MainViewModel extends AndroidViewModel {
    /** Heatmap menampilkan 21 minggu terakhir. */
    public static final int HEAT_WEEKS = 21;

    private final SavingsRepository repo;

    public final LiveData<PlayerEntity> player;
    public final LiveData<List<VaultEntity>> vaults;
    public final LiveData<Long> total;
    public final LiveData<Long> today;
    public final LiveData<Long> month;
    public final LiveData<List<QuestEntity>> quests;
    public final LiveData<List<BadgeEntity>> badges;
    public final LiveData<List<TxWithVault>> recent;
    public final LiveData<List<DayTotal>> heat;
    public final LiveData<List<OwnedItemEntity>> owned;
    public final LiveData<List<InventoryEntity>> inventory;
    public final LiveData<List<ShopItemEntity>> shopItems;
    /** Event sekali-tampil untuk UI (haptic + dialog). */
    public final MutableLiveData<SaveResult> result = new MutableLiveData<>();

    public MainViewModel(@NonNull Application app) {
        super(app);
        repo = new SavingsRepository(app);
        long d = DateUtil.today();
        player = repo.player();
        vaults = repo.vaults();
        total = repo.totalSaved();
        today = repo.depositSum(d, d);
        month = repo.depositSum(DateUtil.monthStart(), DateUtil.monthEnd());
        quests = repo.activeQuests(d);
        badges = repo.badges();
        recent = repo.recent(6);
        heat = repo.dailyTotals(DateUtil.weekStart(d) - (HEAT_WEEKS - 1) * 7L, d);
        owned = repo.ownedItems();
        inventory = repo.inventory();
        shopItems = repo.shopItems();
    }

    public void refresh() { repo.refreshAsync(result::setValue); }

    /** Menyalurkan hasil yang sudah selesai (mis. spin setelah animasi roda berhenti). */
    public void deliver(SaveResult r) { result.setValue(r); }

    public void createVault(String name, long target, int lockDays, String type, int bossKind) {
        repo.createVault(name, target, lockDays, type, bossKind, result::setValue);
    }

    public void deleteVault(VaultEntity v) { repo.deleteVault(v, result::setValue); }

    public void deposit(long vaultId, long amount, String note) {
        repo.deposit(vaultId, amount, TransactionEntity.DEPOSIT, note, result::setValue);
    }

    public void withdraw(long vaultId, long amount, String note, boolean force) {
        repo.withdraw(vaultId, amount, note, force, result::setValue);
    }

    public void claimQuest(long questId) { repo.claimQuest(questId, result::setValue); }

    public void spin(SavingsRepository.Callback cb) { repo.spin(cb); }

    public void buy(String itemId) { repo.buyItem(itemId, result::setValue); }

    /** Beli item habis-pakai (consumable) sebanyak qty. */
    public void buyConsumable(String itemId, int qty) { repo.buyShopItem(itemId, qty, result::setValue); }

    /** Gunakan 1 item dari Ransel. */
    public void useItem(String itemId) { repo.useItem(itemId, result::setValue); }

    public void equip(String itemId) { repo.equipItem(itemId, result::setValue); }

    public void setAvatar(String id) { repo.setAvatar(id, result::setValue); }

    public void setCustomAvatar(String path) { repo.setCustomAvatar(path, result::setValue); }

    public void roundUp(long vaultId, long spent, long step) {
        long change = RoundUp.change(spent, step);
        if (change <= 0) {
            SaveResult r = new SaveResult();
            r.error = "Tidak ada kembalian untuk disisihkan";
            result.setValue(r);
            return;
        }
        repo.deposit(vaultId, change, TransactionEntity.ROUNDUP,
                "Pembulatan belanja " + Money.rp(spent), result::setValue);
    }
}
