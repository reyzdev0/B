package com.savequest.app.ui.badges;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.savequest.app.R;
import com.savequest.app.data.local.entity.BadgeEntity;
import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.domain.BadgeCatalog;
import com.savequest.app.domain.BadgeEngine;
import com.savequest.app.ui.common.DialogFactory;
import com.savequest.app.ui.common.Ui;
import com.savequest.app.ui.main.MainViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BadgesFragment extends Fragment {
    private BadgeAdapter adapter;
    private TextView tvCount;
    private ProgressBar bar;
    private PlayerEntity player;
    private Map<String, Long> unlocked = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_badges, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        super.onViewCreated(v, state);
        tvCount = v.findViewById(R.id.tvBadgeCount);
        bar = v.findViewById(R.id.badgeBar);
        ImageView header = v.findViewById(R.id.ivBadgeHeader);
        int[] gold = Ui.tier(3);
        header.setBackground(Ui.oval(gold[0], gold[1]));
        header.setColorFilter(0xFFFFFFFF);

        adapter = new BadgeAdapter((def, un, at) -> {
            String progress = null;
            if (!un && player != null && def.hasProgress()) {
                progress = def.progressText(BadgeEngine.metric(def.trigger, player));
            }
            DialogFactory.badgeInfo(requireContext(), def, un, at, progress).show();
        });
        GridLayoutManager lm = new GridLayoutManager(requireContext(), 3);
        lm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isHeader(position) ? 3 : 1;
            }
        });
        RecyclerView rv = v.findViewById(R.id.rvBadges);
        rv.setLayoutManager(lm);
        rv.setAdapter(adapter);

        // pengelompokan: per kategori atau per tier
        final TextView byCat = v.findViewById(R.id.chipByCategory);
        final TextView byTier = v.findViewById(R.id.chipByTier);
        Ui.styleChip(byCat, true);
        Ui.styleChip(byTier, false);
        byCat.setOnClickListener(x -> {
            Ui.styleChip(byCat, true);
            Ui.styleChip(byTier, false);
            adapter.setMode(BadgeAdapter.BY_CATEGORY);
        });
        byTier.setOnClickListener(x -> {
            Ui.styleChip(byCat, false);
            Ui.styleChip(byTier, true);
            adapter.setMode(BadgeAdapter.BY_TIER);
        });

        MainViewModel vm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        vm.player.observe(getViewLifecycleOwner(), (PlayerEntity p) -> {
            player = p;
            adapter.submit(unlocked, player);
        });
        vm.badges.observe(getViewLifecycleOwner(), (List<BadgeEntity> list) -> {
            Map<String, Long> map = new HashMap<>();
            if (list != null) for (BadgeEntity b : list) if (b.unlockedAt > 0) map.put(b.code, b.unlockedAt);
            unlocked = map;
            int total = BadgeCatalog.ALL.size();
            tvCount.setText(map.size() + " / " + total + " badge terbuka");
            bar.setProgress(map.size() * 100 / total);
            adapter.submit(unlocked, player);
        });
    }
}
