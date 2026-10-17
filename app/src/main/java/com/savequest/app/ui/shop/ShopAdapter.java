package com.savequest.app.ui.shop;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.savequest.app.R;
import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.domain.CosmeticCatalog;
import com.savequest.app.ui.common.Ui;
import com.savequest.app.ui.views.AvatarRenderer;
import com.savequest.app.ui.views.AvatarView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Daftar item Toko. Pratinjau memakai avatar kamu dengan item tersebut terpasang. */
public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.VH> {
    public interface Listener {
        void onBuy(CosmeticCatalog.Item item);
        void onEquip(CosmeticCatalog.Item item);
    }

    private List<CosmeticCatalog.Item> items = new ArrayList<>();
    private PlayerEntity player;
    private Set<String> owned = new HashSet<>();
    private final Listener listener;

    public ShopAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<CosmeticCatalog.Item> items, PlayerEntity player, Set<String> owned) {
        this.items = items;
        this.player = player;
        this.owned = owned;
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        final AvatarView preview;
        final TextView name, price, action;
        final View coin;

        VH(View v) {
            super(v);
            preview = v.findViewById(R.id.shopPreview);
            name = v.findViewById(R.id.shopName);
            price = v.findViewById(R.id.shopPrice);
            action = v.findViewById(R.id.shopAction);
            coin = v.findViewById(R.id.shopCoinIcon);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop, parent, false));
    }

    private boolean isEquipped(CosmeticCatalog.Item it) {
        String cur;
        if (CosmeticCatalog.FRAME.equals(it.category)) cur = player.frameId;
        else if (CosmeticCatalog.SKIN.equals(it.category)) cur = player.skinId;
        else cur = player.bgId;
        return it.id.equals(cur);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        if (player == null) return;
        CosmeticCatalog.Item it = items.get(position);
        boolean has = it.isFree() || owned.contains(it.id);
        boolean equipped = isEquipped(it);

        AvatarRenderer.Appearance a = AvatarRenderer.Appearance.from(player);
        if (CosmeticCatalog.FRAME.equals(it.category)) a.frameId = it.id;
        else if (CosmeticCatalog.SKIN.equals(it.category)) a.skinId = it.id;
        else a.bgId = it.id;
        h.preview.setAppearance(a);

        h.name.setText(it.name);
        h.coin.setVisibility(has ? View.GONE : View.VISIBLE);
        h.price.setText(has ? "Dimiliki" : String.valueOf(it.price));

        if (equipped) {
            h.action.setText("Dipakai");
            h.action.setBackgroundResource(R.drawable.bg_btn_ghost);
            h.action.setTextColor(Ui.color(h.itemView.getContext(), R.color.sq_text_dim));
            h.action.setOnClickListener(null);
            h.action.setClickable(false);
        } else {
            h.action.setText(has ? "Pakai" : "Beli");
            h.action.setBackgroundResource(R.drawable.bg_btn_primary);
            h.action.setTextColor(0xFFFFFFFF);
            h.action.setClickable(true);
            h.action.setOnClickListener(x -> {
                if (has) listener.onEquip(it); else listener.onBuy(it);
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
