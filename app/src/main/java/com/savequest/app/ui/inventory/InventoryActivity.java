package com.savequest.app.ui.inventory;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.savequest.app.R;
import com.savequest.app.data.local.entity.InventoryEntity;
import com.savequest.app.data.local.entity.OwnedItemEntity;
import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.data.local.entity.ShopItemEntity;
import com.savequest.app.domain.CosmeticCatalog;
import com.savequest.app.domain.HpSystem;
import com.savequest.app.domain.ItemCatalog;
import com.savequest.app.domain.ItemEffectManager;
import com.savequest.app.domain.StreakSystem;
import com.savequest.app.ui.common.ItemIcons;
import com.savequest.app.ui.common.Ui;
import com.savequest.app.ui.main.MainViewModel;
import com.savequest.app.ui.main.ResultPresenter;
import com.savequest.app.ui.main.SecureActivity;
import com.savequest.app.util.DateUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ransel: grid item dengan filter kategori, jumlah (quantity), dan tombol Gunakan Item. */
public class InventoryActivity extends SecureActivity {
    private static final String ALL = "ALL";

    private MainViewModel vm;
    private InventoryAdapter adapter;
    private ResultPresenter presenter;
    private TextView tvCount, tvEffects, tvEmpty, tvSelName, tvSelMeta, tvSelDesc, btnUse;
    private View selPanel;
    private LinearLayout filterRow;

    private PlayerEntity player;
    private List<InventoryEntity> inventory = new ArrayList<>();
    private List<OwnedItemEntity> owned = new ArrayList<>();
    private String filter = ALL;
    private String selectedId;
    private final List<InvEntry> shown = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);
        vm = new ViewModelProvider(this).get(MainViewModel.class);
        presenter = new ResultPresenter(this, vm);

        tvCount = findViewById(R.id.tvInvCount);
        tvEffects = findViewById(R.id.tvEffects);
        tvEmpty = findViewById(R.id.tvInvEmpty);
        tvSelName = findViewById(R.id.tvSelName);
        tvSelMeta = findViewById(R.id.tvSelMeta);
        tvSelDesc = findViewById(R.id.tvSelDesc);
        btnUse = findViewById(R.id.btnUse);
        selPanel = findViewById(R.id.selPanel);
        filterRow = findViewById(R.id.filterRow);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        adapter = new InventoryAdapter(e -> {
            selectedId = e.id;
            refresh();
        });
        RecyclerView rv = findViewById(R.id.rvInventory);
        rv.setLayoutManager(new GridLayoutManager(this, 4));
        rv.setAdapter(adapter);

        buildFilters();

        vm.player.observe(this, p -> {
            player = p;
            refresh();
        });
        vm.inventory.observe(this, list -> {
            inventory = list == null ? new ArrayList<>() : list;
            refresh();
        });
        vm.owned.observe(this, list -> {
            owned = list == null ? new ArrayList<>() : list;
            refresh();
        });
        vm.result.observe(this, r -> {
            if (r == null) return;
            vm.result.setValue(null);
            presenter.present(r);
        });
    }

    private void buildFilters() {
        final String[] keys = {ALL, ItemCatalog.CAT_HEAL, ItemCatalog.CAT_PROTECT, ItemCatalog.CAT_BOOST,
                ItemCatalog.CAT_SPECIAL, InvEntry.CAT_COSMETIC};
        final String[] names = {"Semua", "Pemulihan", "Pelindung", "Booster", "Spesial", "Kosmetik"};
        final TextView[] chips = new TextView[keys.length];
        for (int i = 0; i < keys.length; i++) {
            final int idx = i;
            TextView t = new TextView(this);
            t.setText(names[i]);
            t.setTextSize(13);
            t.setClickable(true);
            t.setFocusable(true);
            t.setPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14), Ui.dp(this, 8));
            Ui.styleChip(t, i == 0);
            chips[i] = t;
            t.setOnClickListener(v -> {
                filter = keys[idx];
                for (int k = 0; k < chips.length; k++) Ui.styleChip(chips[k], k == idx);
                refresh();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMarginEnd(Ui.dp(this, 8));
            filterRow.addView(t, lp);
        }
    }

    private void refresh() {
        if (player == null) return;

        // 1. kumpulkan semua slot: item habis-pakai + kosmetik yang dimiliki
        List<InvEntry> all = new ArrayList<>();
        for (InventoryEntity inv : inventory) {
            ShopItemEntity def = ItemCatalog.find(inv.itemId);
            if (def == null || inv.quantity <= 0) continue;
            InvEntry e = new InvEntry();
            e.id = def.id;
            e.name = def.name;
            e.description = def.description;
            e.category = def.category;
            e.quantity = inv.quantity;
            e.rarity = def.rarity;
            e.iconRes = ItemIcons.res(def.iconKey);
            all.add(e);
        }
        for (OwnedItemEntity o : owned) {
            CosmeticCatalog.Item c = CosmeticCatalog.find(o.itemId);
            if (c == null) continue;
            InvEntry e = new InvEntry();
            e.id = c.id;
            e.name = c.name;
            e.description = "Kosmetik " + categoryName(c.category) + ". Tekan Pakai untuk memasangnya di avatar.";
            e.category = InvEntry.CAT_COSMETIC;
            e.quantity = 1;
            e.rarity = c.price >= 400 ? 4 : c.price >= 200 ? 3 : c.price >= 100 ? 2 : 1;
            e.iconRes = R.drawable.ic_star;
            e.cosmetic = true;
            e.equipped = c.id.equals(player.frameId) || c.id.equals(player.skinId) || c.id.equals(player.bgId);
            all.add(e);
        }

        // 2. filter kategori
        shown.clear();
        for (InvEntry e : all) if (ALL.equals(filter) || filter.equals(e.category)) shown.add(e);

        tvCount.setText(all.size() + " jenis item");
        tvEmpty.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
        adapter.submit(new ArrayList<>(shown), selectedId);

        // 3. panel item terpilih
        InvEntry sel = null;
        for (InvEntry e : all) if (e.id.equals(selectedId)) sel = e;
        updatePanel(sel);
        updateEffects();
    }

    private static String categoryName(String c) {
        if (CosmeticCatalog.FRAME.equals(c)) return "bingkai";
        if (CosmeticCatalog.SKIN.equals(c)) return "skin";
        return "latar";
    }

    private void updatePanel(InvEntry sel) {
        if (sel == null) {
            selPanel.setVisibility(View.GONE);
            return;
        }
        selPanel.setVisibility(View.VISIBLE);
        tvSelName.setText(sel.name);
        tvSelDesc.setText(sel.description);
        if (sel.cosmetic) {
            tvSelMeta.setText("Kosmetik  |  " + ItemCatalog.rarityName(sel.rarity));
            btnUse.setText(sel.equipped ? "Sedang dipakai" : "Pakai");
            btnUse.setEnabled(!sel.equipped);
            btnUse.setAlpha(sel.equipped ? 0.5f : 1f);
        } else {
            tvSelMeta.setText(ItemCatalog.categoryLabel(sel.category) + "  |  "
                    + ItemCatalog.rarityName(sel.rarity) + "  |  Jumlah " + sel.quantity);
            boolean passive = ItemCatalog.PROTECT_SCROLL.equals(sel.id) || ItemCatalog.STREAK_SHIELD.equals(sel.id);
            btnUse.setText(passive ? "Aktif otomatis" : "Gunakan Item");
            btnUse.setEnabled(!passive);
            btnUse.setAlpha(passive ? 0.5f : 1f);
        }
        final InvEntry target = sel;
        btnUse.setOnClickListener(v -> {
            if (target.cosmetic) vm.equip(target.id); else vm.useItem(target.id);
        });
    }

    /** Ringkasan semua efek yang sedang aktif (booster, buff roda, perlindungan streak, HP). */
    private void updateEffects() {
        long now = System.currentTimeMillis();
        long today = DateUtil.today();
        StringBuilder sb = new StringBuilder();
        sb.append("HP ").append(player.hp).append(" / ").append(HpSystem.MAX)
                .append(" (").append(HpSystem.status(player.hp)).append(")");

        if (ItemEffectManager.boostActive(player, now)) {
            sb.append("\nBooster EXP ").append(StreakSystem.label(player.boostMult));
            if (player.boostCharges > 0) sb.append(": sisa ").append(player.boostCharges).append(" setoran");
            else sb.append(" sampai ").append(DateUtil.formatDateTime(player.boostUntil));
        } else {
            sb.append("\nBooster EXP: tidak aktif");
        }
        if (today <= player.buffUntilDay && player.buffMultiplier > 1.0) {
            sb.append("\nBuff roda ").append(StreakSystem.label(player.buffMultiplier)).append(" EXP sampai ")
                    .append(DateUtil.formatDate(DateUtil.dayMillis(player.buffUntilDay)));
        }
        Map<String, Integer> qty = new HashMap<>();
        for (InventoryEntity i : inventory) qty.put(i.itemId, i.quantity);
        int cover = StreakSystem.cover(qty.containsKey(ItemCatalog.STREAK_SHIELD) ? qty.get(ItemCatalog.STREAK_SHIELD) : 0,
                qty.containsKey(ItemCatalog.PROTECT_SCROLL) ? qty.get(ItemCatalog.PROTECT_SCROLL) : 0);
        sb.append("\nPerlindungan streak: ").append(cover > 0 ? "menutup sampai " + cover + " hari terlewat" : "tidak ada");
        tvEffects.setText(sb.toString());
    }
}
