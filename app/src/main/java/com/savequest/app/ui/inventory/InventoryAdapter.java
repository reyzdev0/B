package com.savequest.app.ui.inventory;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.savequest.app.R;
import com.savequest.app.domain.ItemCatalog;
import com.savequest.app.ui.views.ItemSlotView;

import java.util.ArrayList;
import java.util.List;

/** Grid slot Ransel. */
public class InventoryAdapter extends RecyclerView.Adapter<InventoryAdapter.VH> {
    public interface OnSelect {
        void onSelect(InvEntry entry);
    }

    private List<InvEntry> items = new ArrayList<>();
    private String selectedId;
    private final OnSelect listener;

    public InventoryAdapter(OnSelect listener) {
        this.listener = listener;
    }

    public void submit(List<InvEntry> list, String selectedId) {
        this.items = list;
        this.selectedId = selectedId;
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemSlotView slot;
        final TextView name;

        VH(View v) {
            super(v);
            slot = v.findViewById(R.id.invSlot);
            name = v.findViewById(R.id.invName);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_inventory, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        InvEntry e = items.get(position);
        boolean selected = e.id.equals(selectedId);
        h.slot.set(e.iconRes, ItemCatalog.rarityColor(e.rarity), e.cosmetic ? -1 : e.quantity, selected, false);
        h.name.setText(e.name);
        h.itemView.setOnClickListener(x -> listener.onSelect(e));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
