package com.savequest.app.ui.home;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.savequest.app.R;
import com.savequest.app.data.local.entity.VaultEntity;
import com.savequest.app.util.DateUtil;
import com.savequest.app.util.Money;

import java.util.ArrayList;
import java.util.List;

public class VaultAdapter extends RecyclerView.Adapter<VaultAdapter.VH> {
    public interface OnVaultClick {
        void onClick(VaultEntity vault);
    }

    private List<VaultEntity> items = new ArrayList<>();
    private final OnVaultClick listener;

    public VaultAdapter(OnVaultClick listener) {
        this.listener = listener;
    }

    public void submit(List<VaultEntity> list) {
        items = list == null ? new ArrayList<>() : list;
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, lock, amount, pct;
        final View lockChip;
        final ProgressBar bar;
        final ImageView type;

        VH(View v) {
            super(v);
            name = v.findViewById(R.id.tvVaultName);
            lock = v.findViewById(R.id.tvVaultLock);
            lockChip = v.findViewById(R.id.lockChip);
            amount = v.findViewById(R.id.tvVaultAmount);
            pct = v.findViewById(R.id.tvVaultPct);
            bar = v.findViewById(R.id.vaultBar);
            type = v.findViewById(R.id.ivVaultType);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vault, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        VaultEntity v = items.get(position);
        int pct = v.targetAmount > 0
                ? (int) Math.min(100, v.savedAmount * 100 / v.targetAmount) : 0;
        int color = pct < 30 ? 0xFFE53935 : pct < 70 ? 0xFFFB8C00 : 0xFF43A047;

        boolean boss = VaultEntity.TYPE_BOSS.equals(v.type);
        boolean emergency = VaultEntity.TYPE_EMERGENCY.equals(v.type);
        h.type.setImageResource(boss ? R.drawable.ic_skull : emergency ? R.drawable.ic_alert : R.drawable.ic_vault);
        h.type.setColorFilter(boss ? 0xFFE53935 : emergency ? 0xFFFB8C00 : 0xFF2E9D4F);
        h.name.setText(v.name);
        h.amount.setText(Money.rp(v.savedAmount) + " / " + Money.rp(v.targetAmount));
        h.pct.setText(pct + "%");
        h.pct.setTextColor(color);
        h.bar.setProgress(pct);
        h.bar.setProgressTintList(ColorStateList.valueOf(color));

        boolean locked = v.lockUntil > System.currentTimeMillis();
        h.lockChip.setVisibility(locked ? View.VISIBLE : View.GONE);
        if (locked) h.lock.setText(DateUtil.formatDate(v.lockUntil));
        h.itemView.setOnClickListener(x -> listener.onClick(v));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
