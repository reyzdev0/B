package com.savequest.app.ui.badges;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.savequest.app.R;
import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.domain.BadgeCatalog;
import com.savequest.app.domain.BadgeEngine;
import com.savequest.app.ui.common.Ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Grid badge, dikelompokkan per kategori atau per tier (Bronze sampai Diamond). */
public class BadgeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface OnBadgeClick {
        void onClick(BadgeCatalog.Def def, boolean unlocked, long unlockedAt);
    }

    public static final int BY_CATEGORY = 0, BY_TIER = 1;
    private static final int TYPE_HEADER = 0, TYPE_BADGE = 1;

    private final List<Object> items = new ArrayList<>();
    private Map<String, Long> unlocked = new HashMap<>();
    private PlayerEntity player;
    private int mode = BY_CATEGORY;
    private final OnBadgeClick listener;

    public BadgeAdapter(OnBadgeClick listener) {
        this.listener = listener;
        rebuild();
    }

    public void submit(Map<String, Long> unlocked, PlayerEntity player) {
        this.unlocked = unlocked;
        this.player = player;
        rebuild();
        notifyDataSetChanged();
    }

    public void setMode(int mode) {
        this.mode = mode;
        rebuild();
        notifyDataSetChanged();
    }

    public boolean isHeader(int position) {
        return items.get(position) instanceof String;
    }

    private void addGroup(String title, List<BadgeCatalog.Def> group) {
        if (group.isEmpty()) return;
        int got = 0;
        for (BadgeCatalog.Def d : group) if (unlocked.containsKey(d.code)) got++;
        items.add(title + "  (" + got + "/" + group.size() + ")");
        items.addAll(group);
    }

    private void rebuild() {
        items.clear();
        if (mode == BY_CATEGORY) {
            for (String cat : BadgeCatalog.CATEGORIES) {
                List<BadgeCatalog.Def> g = new ArrayList<>();
                for (BadgeCatalog.Def d : BadgeCatalog.ALL) if (d.category.equals(cat)) g.add(d);
                addGroup(cat, g);
            }
        } else {
            for (int tier : BadgeCatalog.TIERS) {
                List<BadgeCatalog.Def> g = new ArrayList<>();
                for (BadgeCatalog.Def d : BadgeCatalog.ALL) if (d.tier == tier) g.add(d);
                addGroup(BadgeCatalog.tierName(tier), g);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return isHeader(position) ? TYPE_HEADER : TYPE_BADGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new RecyclerView.ViewHolder(inf.inflate(R.layout.item_badge_header, parent, false)) {};
        }
        return new RecyclerView.ViewHolder(inf.inflate(R.layout.item_badge, parent, false)) {};
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
        View v = h.itemView;
        Object item = items.get(position);
        if (item instanceof String) {
            ((TextView) v).setText((String) item);
            return;
        }

        BadgeCatalog.Def d = (BadgeCatalog.Def) item;
        boolean un = unlocked.containsKey(d.code);
        long at = un ? unlocked.get(d.code) : 0;

        ImageView icon = v.findViewById(R.id.ivBadge);
        ImageView lock = v.findViewById(R.id.ivBadgeLock);
        TextView title = v.findViewById(R.id.tvBadgeTitle);
        TextView sub = v.findViewById(R.id.tvBadgeSub);

        if (un) {
            int[] col = Ui.tier(d.tier);
            icon.setBackground(Ui.oval(col[0], col[1]));
            icon.setImageResource(d.iconRes);
            icon.setColorFilter(0xFFFFFFFF);
            icon.setElevation(Ui.dp(v.getContext(), 4));
            lock.setVisibility(View.GONE);
            title.setText(d.title);
            sub.setText(BadgeCatalog.tierName(d.tier));
        } else {
            int locked = Ui.color(v.getContext(), R.color.sq_locked);
            icon.setBackground(Ui.oval(locked, locked));
            icon.setImageResource(d.secret ? R.drawable.ic_lock : d.iconRes);
            icon.setColorFilter(Ui.color(v.getContext(), R.color.sq_text_dim));
            icon.setElevation(0);
            lock.setVisibility(d.secret ? View.GONE : View.VISIBLE);
            title.setText(d.secret ? "???" : d.title);
            sub.setText(progressShort(d));
        }
        v.setOnClickListener(x -> listener.onClick(d, un, at));
    }

    private String progressShort(BadgeCatalog.Def d) {
        if (player == null || !d.hasProgress()) return "";
        long m = BadgeEngine.metric(d.trigger, player);
        if (d.isMoney()) return Math.min(100, m * 100 / d.threshold) + "%";
        return Math.min(m, d.threshold) + "/" + d.threshold;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
