package com.savequest.app.ui.shop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.savequest.app.R;
import com.savequest.app.data.local.entity.ShopItemEntity;
import com.savequest.app.domain.ItemCatalog;
import com.savequest.app.ui.common.ItemIcons;
import com.savequest.app.ui.views.ItemSlotView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Daftar item habis-pakai di Toko (Perban, Perisai Streak, Ramuan EXP, dll). */
public class ConsumableShopAdapter extends RecyclerView.Adapter<ConsumableShopAdapter.VH> {
    public interface Listener {
        void onBuy(ShopItemEntity item, int owned);
    }

    private List<ShopItemEntity> items = new ArrayList<>();
    private Map<String, Integer> owned = new HashMap<>();
    private long coins;
    private final Listener listener;

    public ConsumableShopAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<ShopItemEntity> items, Map<String, Integer> owned, long coins) {
        this.items = items;
        this.owned = owned;
        this.coins = coins;
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemSlotView slot;
        final TextView name, desc, ownedText, price, buy;

        VH(View v) {
            super(v);
            slot = v.findViewById(R.id.csSlot);
            name = v.findViewById(R.id.csName);
            desc = v.findViewById(R.id.csDesc);
            ownedText = v.findViewById(R.id.csOwned);
            price = v.findViewById(R.id.csPrice);
            buy = v.findViewById(R.id.csBuy);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop_consumable, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        ShopItemEntity it = items.get(position);
        Integer have = owned.get(it.id);
        int qty = have == null ? 0 : have;

        h.slot.set(ItemIcons.res(it.iconKey), ItemCatalog.rarityColor(it.rarity), -1, false, false);
        h.name.setText(it.name);
        h.desc.setText(it.description);
        h.ownedText.setText(ItemCatalog.rarityName(it.rarity) + "  |  Kamu punya: " + qty);
        h.price.setText(String.valueOf(it.price));
        h.buy.setAlpha(coins >= it.price ? 1f : 0.55f);
        h.buy.setOnClickListener(x -> listener.onBuy(it, qty));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
