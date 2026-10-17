package com.savequest.app.ui.home;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.savequest.app.R;
import com.savequest.app.audio.BgmPlayer;
import com.savequest.app.data.local.entity.BadgeEntity;
import com.savequest.app.data.local.entity.DayTotal;
import com.savequest.app.data.local.entity.InventoryEntity;
import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.data.local.entity.QuestEntity;
import com.savequest.app.data.local.entity.TransactionEntity;
import com.savequest.app.data.local.entity.TxWithVault;
import com.savequest.app.data.local.entity.VaultEntity;
import com.savequest.app.domain.HpSystem;
import com.savequest.app.domain.ItemCatalog;
import com.savequest.app.domain.ItemEffectManager;
import com.savequest.app.domain.LevelSystem;
import com.savequest.app.domain.StreakSystem;
import com.savequest.app.share.ShareCardGenerator;
import com.savequest.app.ui.avatar.AvatarActivity;
import com.savequest.app.ui.common.DialogFactory;
import com.savequest.app.ui.common.SqDialog;
import com.savequest.app.ui.inventory.InventoryActivity;
import com.savequest.app.ui.common.Ui;
import com.savequest.app.ui.main.MainViewModel;
import com.savequest.app.ui.main.SecureActivity;
import com.savequest.app.ui.shop.ShopActivity;
import com.savequest.app.ui.views.AvatarRenderer;
import com.savequest.app.ui.views.BossBattleView;
import com.savequest.app.ui.views.AvatarView;
import com.savequest.app.ui.views.HeatmapView;
import com.savequest.app.util.DateUtil;
import com.savequest.app.util.Money;
import com.savequest.app.util.Session;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Dashboard: avatar/level/HP, aksi cepat, boss battle, quest, heatmap, vault, aktivitas. */
public class HomeFragment extends Fragment {
    private MainViewModel vm;
    private VaultAdapter adapter;
    private PlayerEntity player;
    private List<BadgeEntity> badgeList;
    private VaultEntity bossVault;
    private long todayTotal = 0;
    private int lastExp = 0;
    /** Hari terlewat yang bisa ditutup item pelindung di ransel (0, 1, atau 3). */
    private int shieldCover = 0;

    private TextView tvLevelTitle, tvLevelPill, tvExp, tvHp, tvCoins, tvStreak, tvMultiplier, tvNextStreak, tvBuff;
    private TextView tvToday, tvMonth, tvTotal, tvEmpty, tvRecentEmpty, tvBossHint, spinLabel;
    private ProgressBar expBar, hpBar;
    private AvatarView avatarView;
    private ImageView ivMusic;
    private LinearLayout questBox, recentBox;
    private View bossSection;
    private BossBattleView bossView;
    private HeatmapView heatmap;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        super.onViewCreated(v, state);
        tvLevelTitle = v.findViewById(R.id.tvLevelTitle);
        tvLevelPill = v.findViewById(R.id.tvLevelPill);
        tvExp = v.findViewById(R.id.tvExp);
        tvHp = v.findViewById(R.id.tvHp);
        tvCoins = v.findViewById(R.id.tvCoins);
        tvStreak = v.findViewById(R.id.tvStreak);
        tvMultiplier = v.findViewById(R.id.tvMultiplier);
        tvNextStreak = v.findViewById(R.id.tvNextStreak);
        tvBuff = v.findViewById(R.id.tvBuff);
        tvToday = v.findViewById(R.id.tvToday);
        tvMonth = v.findViewById(R.id.tvMonth);
        tvTotal = v.findViewById(R.id.tvTotal);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        tvRecentEmpty = v.findViewById(R.id.tvRecentEmpty);
        tvBossHint = v.findViewById(R.id.tvBossHint);
        spinLabel = v.findViewById(R.id.actSpinLabel);
        expBar = v.findViewById(R.id.expBar);
        hpBar = v.findViewById(R.id.hpBar);
        avatarView = v.findViewById(R.id.avatarView);
        ivMusic = v.findViewById(R.id.ivMusic);
        questBox = v.findViewById(R.id.questBox);
        recentBox = v.findViewById(R.id.recentBox);
        bossSection = v.findViewById(R.id.bossSection);
        bossView = v.findViewById(R.id.bossView);
        heatmap = v.findViewById(R.id.heatmap);

        RecyclerView rv = v.findViewById(R.id.rvVaults);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new VaultAdapter(this::onVaultClick);
        rv.setAdapter(adapter);

        v.findViewById(R.id.btnAddVault).setOnClickListener(x ->
                DialogFactory.newVault(requireContext(), (name, target, lockDays, type, kind) ->
                        vm.createVault(name, target, lockDays, type, kind)).show());
        avatarView.setOnClickListener(x -> openScreen(AvatarActivity.class));
        v.findViewById(R.id.actShop).setOnClickListener(x -> openScreen(ShopActivity.class));
        v.findViewById(R.id.actBag).setOnClickListener(x -> openScreen(InventoryActivity.class));
        v.findViewById(R.id.actSpin).setOnClickListener(x -> onSpin());
        v.findViewById(R.id.actShare).setOnClickListener(x -> onShare());
        v.findViewById(R.id.actMusic).setOnClickListener(x -> {
            BgmPlayer.get(requireContext()).toggle();
            updateMusicIcon();
        });
        bossView.setOnClickListener(x -> {
            if (bossVault != null) onVaultClick(bossVault);
        });
        heatmap.setOnCellClick((day, total) -> Toast.makeText(requireContext(),
                DateUtil.formatDate(DateUtil.dayMillis(day)) + ": "
                        + (total > 0 ? Money.rp(total) : "belum menabung"), Toast.LENGTH_SHORT).show());
        updateMusicIcon();

        vm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        vm.player.observe(getViewLifecycleOwner(), this::renderPlayer);
        vm.vaults.observe(getViewLifecycleOwner(), list -> {
            adapter.submit(list);
            tvEmpty.setVisibility(list == null || list.isEmpty() ? View.VISIBLE : View.GONE);
            renderBoss(list);
        });
        vm.today.observe(getViewLifecycleOwner(), n -> {
            todayTotal = n == null ? 0 : n;
            tvToday.setText(Money.rp(todayTotal));
            updateSpinTile();
        });
        vm.month.observe(getViewLifecycleOwner(), n -> tvMonth.setText(Money.rp(n == null ? 0 : n)));
        vm.total.observe(getViewLifecycleOwner(), n -> tvTotal.setText(Money.rp(n == null ? 0 : n)));
        vm.quests.observe(getViewLifecycleOwner(), this::renderQuests);
        vm.recent.observe(getViewLifecycleOwner(), this::renderRecent);
        vm.badges.observe(getViewLifecycleOwner(), list -> badgeList = list);
        vm.inventory.observe(getViewLifecycleOwner(), (List<InventoryEntity> list) -> {
            int shields = 0, scrolls = 0;
            if (list != null) {
                for (InventoryEntity e : list) {
                    if (ItemCatalog.STREAK_SHIELD.equals(e.itemId)) shields = e.quantity;
                    if (ItemCatalog.PROTECT_SCROLL.equals(e.itemId)) scrolls = e.quantity;
                }
            }
            shieldCover = StreakSystem.cover(shields, scrolls);
            if (player != null) renderPlayer(player);
        });
        vm.heat.observe(getViewLifecycleOwner(), (List<DayTotal> list) -> {
            Map<Long, Long> map = new HashMap<>();
            if (list != null) for (DayTotal d : list) map.put(d.day, d.total);
            heatmap.setData(map);
        });
    }

    private void openScreen(Class<?> cls) {
        ((SecureActivity) requireActivity()).startInternal(new Intent(requireContext(), cls));
    }

    private void updateMusicIcon() {
        boolean on = BgmPlayer.get(requireContext()).isEnabled();
        ivMusic.setImageResource(on ? R.drawable.ic_music : R.drawable.ic_music_off);
    }

    // ---------- render ----------
    private void renderPlayer(PlayerEntity p) {
        if (p == null) return;
        player = p;
        int level = LevelSystem.levelFor(p.totalExp);
        avatarView.setAppearance(AvatarRenderer.Appearance.from(p));
        tvLevelPill.setText("Lv " + level);
        tvLevelTitle.setText(LevelSystem.title(level));
        tvExp.setText(p.totalExp + " / " + LevelSystem.expForLevel(level + 1) + " EXP");
        tvCoins.setText(String.valueOf(p.coins));

        int target = LevelSystem.progressPermille(p.totalExp);
        ObjectAnimator a = ObjectAnimator.ofInt(expBar, "progress", lastExp, target);
        a.setDuration(600);
        a.start();
        lastExp = target;

        hpBar.setProgress(p.hp);
        hpBar.setProgressTintList(ColorStateList.valueOf(HpSystem.color(p.hp)));
        tvHp.setText("HP " + p.hp + " / " + HpSystem.MAX + "  |  " + HpSystem.status(p.hp));

        long today = DateUtil.today();
        int streak = StreakSystem.effective(p.streak, p.lastDepositDay, today, shieldCover);
        tvStreak.setText(streak + " hari");
        tvMultiplier.setText(StreakSystem.label(StreakSystem.multiplier(streak)) + " EXP");
        int next = StreakSystem.nextMilestone(streak);
        tvNextStreak.setText(next > 0
                ? (next - streak) + " hari lagi ke " + StreakSystem.label(StreakSystem.multiplier(next))
                : "Multiplier maksimum tercapai");

        StringBuilder fx = new StringBuilder();
        if (today <= p.buffUntilDay && p.buffMultiplier > 1.0) {
            fx.append("Buff roda ").append(StreakSystem.label(p.buffMultiplier)).append(" EXP sampai ")
                    .append(DateUtil.formatDate(DateUtil.dayMillis(p.buffUntilDay)));
        }
        if (ItemEffectManager.boostActive(p, System.currentTimeMillis())) {
            if (fx.length() > 0) fx.append("\n");
            fx.append("Booster EXP ").append(StreakSystem.label(p.boostMult));
            if (p.boostCharges > 0) fx.append(" (").append(p.boostCharges).append(" setoran lagi)");
            else fx.append(" (sampai ").append(DateUtil.formatDateTime(p.boostUntil)).append(")");
        }
        if (shieldCover > 0) {
            if (fx.length() > 0) fx.append("\n");
            fx.append("Perlindungan streak aktif (").append(shieldCover).append(" hari)");
        }
        tvBuff.setVisibility(fx.length() > 0 ? View.VISIBLE : View.GONE);
        tvBuff.setText(fx.toString());
        updateSpinTile();
    }

    private void updateSpinTile() {
        if (spinLabel == null) return;
        boolean ready = player != null && player.lastSpinDay != DateUtil.today() && todayTotal > 0;
        spinLabel.setText(ready ? "Spin siap" : "Spin");
        spinLabel.setTextColor(ready ? Ui.color(requireContext(), R.color.sq_green_2)
                : Ui.color(requireContext(), R.color.sq_text_dim));
    }

    private void renderBoss(List<VaultEntity> list) {
        bossVault = null;
        if (list != null) {
            for (VaultEntity v : list) {
                if (!VaultEntity.TYPE_BOSS.equals(v.type)) continue;
                if (v.defeatedAt == 0) { bossVault = v; break; }
                if (bossVault == null) bossVault = v;
            }
        }
        bossSection.setVisibility(bossVault == null ? View.GONE : View.VISIBLE);
        if (bossVault == null) return;
        VaultEntity b = bossVault;
        bossView.setBoss(b.bossKind, b.name, b.savedAmount, b.targetAmount, b.defeatedAt > 0);
        tvBossHint.setText(b.defeatedAt > 0
                ? "Boss ini sudah tumbang. Buat boss baru untuk tantangan berikutnya."
                : "Setor untuk menyerang! Sisa HP boss: " + Money.rp(Math.max(0, b.targetAmount - b.savedAmount)));
    }

    private void renderQuests(List<QuestEntity> list) {
        questBox.removeAllViews();
        if (list == null) return;
        LayoutInflater inf = LayoutInflater.from(requireContext());
        for (QuestEntity q : list) {
            View row = inf.inflate(R.layout.item_quest, questBox, false);
            ImageView icon = row.findViewById(R.id.ivQuest);
            TextView title = row.findViewById(R.id.tvQuestTitle);
            TextView reward = row.findViewById(R.id.tvQuestReward);
            ProgressBar bar = row.findViewById(R.id.questBar);
            TextView claim = row.findViewById(R.id.btnClaim);

            title.setText(q.title);
            int pct = (int) Math.min(100, q.progress * 100 / Math.max(1, q.targetAmount));
            bar.setProgress(pct);
            reward.setText(Money.rp(q.progress) + " / " + Money.rp(q.targetAmount) + "  |  +" + q.rewardExp + " EXP");

            if (q.completed && q.claimed) {
                icon.setImageResource(R.drawable.ic_check);
                bar.setProgressTintList(ColorStateList.valueOf(0xFF43A047));
                reward.setText("Hadiah sudah diklaim");
            } else if (q.completed) {
                claim.setVisibility(View.VISIBLE);
                claim.setOnClickListener(x -> vm.claimQuest(q.id));
                bar.setProgressTintList(ColorStateList.valueOf(0xFFFB8C00));
            }
            questBox.addView(row);
        }
    }

    private void renderRecent(List<TxWithVault> list) {
        recentBox.removeAllViews();
        boolean empty = list == null || list.isEmpty();
        tvRecentEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) return;
        LayoutInflater inf = LayoutInflater.from(requireContext());
        for (TxWithVault t : list) {
            View row = inf.inflate(R.layout.item_recent, recentBox, false);
            boolean in = t.tx.amount > 0;
            ImageView iv = row.findViewById(R.id.ivTx);
            iv.setImageResource(TransactionEntity.ROUNDUP.equals(t.tx.type) ? R.drawable.ic_coin
                    : in ? R.drawable.ic_deposit : R.drawable.ic_withdraw);
            iv.setBackground(Ui.oval(in ? 0xFF66BB6A : 0xFFEF5350, in ? 0xFF2E9D4F : 0xFFC62828));
            ((TextView) row.findViewById(R.id.tvTxTitle)).setText(t.vaultName);
            TextView note = row.findViewById(R.id.tvTxNote);
            if (t.tx.note == null || t.tx.note.isEmpty()) {
                note.setVisibility(View.GONE);
            } else {
                note.setText(t.tx.note);
            }
            TextView amount = row.findViewById(R.id.tvTxAmount);
            amount.setText((in ? "+ " : "- ") + Money.rp(Math.abs(t.tx.amount)));
            amount.setTextColor(in ? 0xFF43A047 : 0xFFE53935);
            ((TextView) row.findViewById(R.id.tvTxDate)).setText(DateUtil.formatDateTime(t.tx.createdAt));
            recentBox.addView(row);
        }
    }

    // ---------- spin & share ----------
    private void onSpin() {
        Context c = requireContext();
        if (player == null) return;
        if (player.lastSpinDay == DateUtil.today()) {
            Toast.makeText(c, "Kamu sudah memutar roda hari ini. Kembali lagi besok!", Toast.LENGTH_SHORT).show();
        } else if (todayTotal <= 0) {
            Toast.makeText(c, "Setor dulu hari ini untuk membuka roda keberuntungan", Toast.LENGTH_SHORT).show();
        } else {
            SpinDialog.create(c, vm).show();
        }
    }

    private void onShare() {
        if (player == null) return;
        final Context app = requireContext().getApplicationContext();
        final android.app.Activity act = requireActivity();
        final PlayerEntity p = player;
        final List<BadgeEntity> badges = badgeList;
        new Thread(() -> {
            Bitmap card = ShareCardGenerator.render(app, p, badges);
            act.runOnUiThread(() -> {
                if (!isAdded()) return;
                DialogFactory.sharePreview(act, card, () -> {
                    try {
                        android.net.Uri uri = ShareCardGenerator.save(app, card);
                        Session.markExternal();
                        startActivity(ShareCardGenerator.chooser(app, uri));
                    } catch (IOException e) {
                        Toast.makeText(app, "Gagal membuat kartu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).show();
            });
        }).start();
    }

    // ---------- aksi vault ----------
    private int projectedStreak() {
        if (player == null) return 1;
        return StreakSystem.projected(player.streak, player.lastDepositDay, DateUtil.today(), shieldCover);
    }

    /** Buff roda x faktor HP: pengali EXP tambahan di luar streak. */
    private double extraMult() {
        if (player == null) return 1.0;
        double spin = DateUtil.today() <= player.buffUntilDay ? player.buffMultiplier : 1.0;
        double boost = ItemEffectManager.boostMultiplier(player, System.currentTimeMillis());
        return Math.max(spin, boost) * HpSystem.expMultiplier(player.hp);
    }

    private void onVaultClick(VaultEntity v) {
        Context c = requireContext();
        DialogFactory.vaultMenu(c, v, action -> {
            switch (action) {
                case DialogFactory.ACTION_DEPOSIT:
                    DialogFactory.transaction(c, v, true, projectedStreak(), extraMult(),
                            (amount, note) -> vm.deposit(v.id, amount, note)).show();
                    break;
                case DialogFactory.ACTION_WITHDRAW:
                    DialogFactory.transaction(c, v, false, projectedStreak(), extraMult(), (amount, note) -> {
                        boolean lockedEmergency = VaultEntity.TYPE_EMERGENCY.equals(v.type)
                                && v.lockUntil > System.currentTimeMillis();
                        if (lockedEmergency) {
                            DialogFactory.emergencyConfirm(c, v, amount,
                                    () -> vm.withdraw(v.id, amount, note, true)).show();
                        } else {
                            vm.withdraw(v.id, amount, note, false);
                        }
                    }).show();
                    break;
                case DialogFactory.ACTION_ROUNDUP:
                    DialogFactory.roundUp(c, v, (spent, step) -> vm.roundUp(v.id, spent, step)).show();
                    break;
                default:
                    DialogFactory.confirm(c, "Hapus vault?", "Seluruh riwayat vault ini ikut terhapus.",
                            SqDialog.RED, "Hapus", () -> vm.deleteVault(v)).show();
            }
        }).show();
    }
}
