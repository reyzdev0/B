package com.savequest.app.ui.shop;

import android.os.Bundle;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.savequest.app.R;
import com.savequest.app.data.local.entity.InventoryEntity;
import com.savequest.app.data.local.entity.OwnedItemEntity;
import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.data.local.entity.ShopItemEntity;
import com.savequest.app.domain.CosmeticCatalog;
import com.savequest.app.domain.ItemCatalog;
import com.savequest.app.ui.common.DialogFactory;
import com.savequest.app.ui.common.SqDialog;
import com.savequest.app.ui.common.Ui;
import com.savequest.app.ui.main.MainViewModel;
import com.savequest.app.ui.main.ResultPresenter;
import com.savequest.app.ui.main.SecureActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Toko: tab "Item" menjual consumable (Perban, Ramuan, Perisai Streak, dll) yang masuk ke Ransel.
 * Tab lain menjual kosmetik permanen (bingkai, skin, latar). Semua dibayar dengan koin.
 */
public class ShopActivity extends SecureActivity {
    private static final String TAB_ITEMS = "ITEMS";

    private MainViewModel vm;
    private ShopAdapter cosmeticAdapter;
    private ConsumableShopAdapter consumableAdapter;
    private RecyclerView rv;
    private ResultPresenter presenter;
    private TextView tvCoins, tabItems, tabFrame, tabSkin, tabBg;

    private PlayerEntity player;
    private Set<String> ownedCosmetics = new HashSet<>();
    private Map<String, Integer> inventoryQty = new HashMap<>();
    private List<ShopItemEntity> shopItems = new ArrayList<>();
    private String tab = TAB_ITEMS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);
        vm = new ViewModelProvider(this).get(MainViewModel.class);
        presenter = new ResultPresenter(this, vm);
        tvCoins = findViewById(R.id.tvShopCoins);
        tabItems = findViewById(R.id.tabItems);
        tabFrame = findViewById(R.id.tabFrame);
        tabSkin = findViewById(R.id.tabSkin);
        tabBg = findViewById(R.id.tabBg);
        rv = findViewById(R.id.rvShop);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        tabItems.setOnClickListener(v -> switchTab(TAB_ITEMS));
        tabFrame.setOnClickListener(v -> switchTab(CosmeticCatalog.FRAME));
        tabSkin.setOnClickListener(v -> switchTab(CosmeticCatalog.SKIN));
        tabBg.setOnClickListener(v -> switchTab(CosmeticCatalog.BG));

        cosmeticAdapter = new ShopAdapter(new ShopAdapter.Listener() {
            @Override
            public void onBuy(CosmeticCatalog.Item item) {
                DialogFactory.confirm(ShopActivity.this, "Beli " + item.name + "?",
                        "Harga " + item.price + " koin. Saldo kamu "
                                + (player == null ? 0 : player.coins) + " koin.",
                        SqDialog.GREEN, "Beli", () -> vm.buy(item.id)).show();
            }

            @Override
            public void onEquip(CosmeticCatalog.Item item) {
                vm.equip(item.id);
            }
        });
        consumableAdapter = new ConsumableShopAdapter((item, owned) ->
                DialogFactory.buyItem(this, item, owned, player == null ? 0 : player.coins,
                        qty -> vm.buyConsumable(item.id, qty)).show());

        vm.player.observe(this, p -> {
            player = p;
            if (p != null) tvCoins.setText(String.valueOf(p.coins));
            refresh();
        });
        vm.owned.observe(this, (List<OwnedItemEntity> list) -> {
            Set<String> s = new HashSet<>();
            if (list != null) for (OwnedItemEntity o : list) s.add(o.itemId);
            ownedCosmetics = s;
            refresh();
        });
        vm.inventory.observe(this, (List<InventoryEntity> list) -> {
            Map<String, Integer> m = new HashMap<>();
            if (list != null) for (InventoryEntity e : list) m.put(e.itemId, e.quantity);
            inventoryQty = m;
            refresh();
        });
        vm.shopItems.observe(this, list -> {
            shopItems = list == null || list.isEmpty() ? new ArrayList<>(ItemCatalog.defaults()) : list;
            refresh();
        });
        vm.result.observe(this, r -> {
            if (r == null) return;
            vm.result.setValue(null);
            presenter.present(r);
        });
        switchTab(TAB_ITEMS);
    }

    private void switchTab(String t) {
        tab = t;
        Ui.styleChip(tabItems, TAB_ITEMS.equals(t));
        Ui.styleChip(tabFrame, CosmeticCatalog.FRAME.equals(t));
        Ui.styleChip(tabSkin, CosmeticCatalog.SKIN.equals(t));
        Ui.styleChip(tabBg, CosmeticCatalog.BG.equals(t));
        if (TAB_ITEMS.equals(t)) {
            rv.setLayoutManager(new LinearLayoutManager(this));
            rv.setAdapter(consumableAdapter);
        } else {
            rv.setLayoutManager(new GridLayoutManager(this, 2));
            rv.setAdapter(cosmeticAdapter);
        }
        refresh();
    }

    private void refresh() {
        if (player == null) return;
        if (TAB_ITEMS.equals(tab)) {
            consumableAdapter.submit(shopItems, inventoryQty, player.coins);
        } else {
            cosmeticAdapter.submit(CosmeticCatalog.list(tab), player, ownedCosmetics);
        }
    }
}
