package com.savequest.app.ui.common;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.savequest.app.R;
import com.savequest.app.data.local.entity.QuestEntity;
import com.savequest.app.data.local.entity.ShopItemEntity;
import com.savequest.app.data.local.entity.VaultEntity;
import com.savequest.app.domain.BadgeCatalog;
import com.savequest.app.domain.ExpCalculator;
import com.savequest.app.domain.PenaltySystem;
import com.savequest.app.report.ReportExporter;
import com.savequest.app.domain.ProjectionCalculator;
import com.savequest.app.domain.RoundUp;
import com.savequest.app.domain.StreakSystem;
import com.savequest.app.util.DateUtil;
import com.savequest.app.util.Money;
import com.savequest.app.util.PinStore;

import java.util.List;
import java.util.Random;
import java.util.Locale;

/** Semua dialog SaveQuest. Setiap fungsi mengembalikan Builder; panggil .show(). */
public final class DialogFactory {
    private DialogFactory() {}

    public static final int ACTION_DEPOSIT = 0, ACTION_WITHDRAW = 1, ACTION_ROUNDUP = 2, ACTION_DELETE = 3;

    public interface TxCallback { void accept(long amount, String note); }
    public interface VaultCallback { void accept(String name, long target, int lockDays, String type, int bossKind); }
    public interface ReportCallback { void accept(ReportExporter.Period period, boolean pdf); }
    private interface IndexCallback { void pick(int index); }
    public interface QtyCallback { void accept(int qty); }
    public interface TextCallback { void accept(String text); }
    public interface RoundCallback { void accept(long spent, long step); }
    public interface MenuListener { void onAction(int action); }
    public interface TimeCallback { void accept(int hour, int minute); }
    public interface MessageCallback { void show(String message); }

    // ---------- helper tampilan ----------
    public static EditText field(Context c, String hint, boolean numeric) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setTextSize(15);
        e.setTextColor(Ui.color(c, R.color.sq_text));
        e.setHintTextColor(Ui.color(c, R.color.sq_text_dim));
        e.setBackgroundResource(R.drawable.bg_input);
        int h = Ui.dp(c, 14), v = Ui.dp(c, 12);
        e.setPadding(h, v, h, v);
        if (numeric) e.setInputType(InputType.TYPE_CLASS_NUMBER);
        return e;
    }

    private static EditText pinField(Context c, String hint) {
        EditText e = field(c, hint, false);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(PinStore.LENGTH)});
        return e;
    }

    private static LinearLayout column(Context c, View... views) {
        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < views.length; i++) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) lp.topMargin = Ui.dp(c, 10);
            box.addView(views[i], lp);
        }
        return box;
    }

    private static TextView chip(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(13);
        t.setTypeface(null, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setClickable(true);
        t.setFocusable(true);
        int h = Ui.dp(c, 14), v = Ui.dp(c, 8);
        t.setPadding(h, v, h, v);
        Ui.styleChip(t, false);
        return t;
    }

    private static LinearLayout chipRow(Context c, String[] names, int selected, IndexCallback cb) {
        LinearLayout row = new LinearLayout(c);
        final TextView[] chips = new TextView[names.length];
        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            TextView ch = chip(c, names[i]);
            Ui.styleChip(ch, i == selected);
            chips[i] = ch;
            ch.setOnClickListener(x -> {
                for (int k = 0; k < chips.length; k++) Ui.styleChip(chips[k], k == idx);
                cb.pick(idx);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.setMarginStart(Ui.dp(c, 8));
            row.addView(ch, lp);
        }
        return row;
    }

    private static TextView caption(Context c, String text) {
        TextView t = new TextView(c);
        t.setText(text);
        t.setTextSize(12);
        t.setTextColor(Ui.color(c, R.color.sq_text_dim));
        return t;
    }

    // ---------- setor / tarik ----------
    public static SqDialog.Builder transaction(Context c, VaultEntity v, boolean deposit,
                                               int projectedStreak, double extraMult, TxCallback cb) {
        EditText amount = field(c, "Jumlah (Rp)", true);
        EditText note = field(c, "Catatan (mis. Sisa kembalian beli martabak)", false);
        note.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});

        LinearLayout chips = new LinearLayout(c);
        long[] quick = {5_000, 10_000, 20_000, 50_000};
        String[] names = {"5K", "10K", "20K", "50K"};
        for (int i = 0; i < quick.length; i++) {
            final long val = quick[i];
            TextView ch = chip(c, "+" + names[i]);
            ch.setOnClickListener(x -> {
                long cur = Money.parse(amount.getText().toString());
                amount.setText(String.valueOf(cur + val));
                amount.setSelection(amount.getText().length());
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.setMarginStart(Ui.dp(c, 8));
            chips.addView(ch, lp);
        }

        TextView preview = caption(c, "");
        preview.setTextColor(Ui.color(c, R.color.sq_green_2));
        preview.setTypeface(null, Typeface.BOLD);
        if (deposit) {
            preview.setText("Masukkan nominal untuk melihat perkiraan EXP");
            amount.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int d) {}
                @Override
                public void afterTextChanged(Editable e) {
                    long a = Money.parse(e.toString());
                    if (a <= 0) {
                        preview.setText("Masukkan nominal untuk melihat perkiraan EXP");
                    } else {
                        preview.setText("Perkiraan +" + ExpCalculator.compute(a, StreakSystem.multiplier(projectedStreak), extraMult, 1.0)
                                + " EXP (streak " + StreakSystem.label(StreakSystem.multiplier(projectedStreak)) + ")");
                    }
                }
            });
        } else {
            preview.setText("Saldo vault: " + Money.rp(v.savedAmount));
        }

        LinearLayout body = column(c, amount, chips, preview, note);
        if (!deposit) chips.setVisibility(View.GONE);

        return new SqDialog.Builder(c)
                .icon(deposit ? R.drawable.ic_deposit : R.drawable.ic_withdraw)
                .accent(deposit ? SqDialog.GREEN : SqDialog.RED)
                .title((deposit ? "Setor ke " : "Tarik dari ") + v.name)
                .body(body)
                .negative("Batal", null)
                .positive(deposit ? "Setor" : "Tarik", () -> {
                    long a = Money.parse(amount.getText().toString());
                    if (a <= 0) {
                        amount.setError("Isi nominal");
                        return false;
                    }
                    cb.accept(a, note.getText().toString().trim());
                    return true;
                });
    }

    public static SqDialog.Builder newVault(Context c, VaultCallback cb) {
        EditText name = field(c, "Nama target (mis. Beli Laptop)", false);
        EditText target = field(c, "Target (Rp)", true);
        EditText lock = field(c, "Kunci selama (hari, 0 = tidak dikunci)", true);
        final String[] types = {VaultEntity.TYPE_NORMAL, VaultEntity.TYPE_BOSS, VaultEntity.TYPE_EMERGENCY};
        final int[] selType = {0};
        final int[] selBoss = {0};
        final TextView hint = caption(c, "Vault biasa untuk tabungan sehari-hari.");
        final LinearLayout bossRow = chipRow(c, new String[]{"Slime", "Golem", "Naga"}, 0, i -> selBoss[0] = i);
        bossRow.setVisibility(View.GONE);
        final String[] hints = {
                "Vault biasa untuk tabungan sehari-hari.",
                "Boss Battle: target jadi HP boss. Tiap setoran adalah serangan. Boss kalah saat target tercapai.",
                "Dana darurat: terkunci rapat (default 90 hari). Ditarik sebelum tenggat = penalti EXP dan HP."};
        LinearLayout typeRow = chipRow(c, new String[]{"Biasa", "Boss", "Darurat"}, 0, i -> {
            selType[0] = i;
            hint.setText(hints[i]);
            bossRow.setVisibility(i == 1 ? View.VISIBLE : View.GONE);
        });
        return new SqDialog.Builder(c)
                .icon(R.drawable.ic_vault)
                .title("Vault baru")
                .body(column(c, caption(c, "Jenis"), typeRow, hint, bossRow, name, target, lock))
                .negative("Batal", null)
                .positive("Buat", () -> {
                    String n = name.getText().toString().trim();
                    long t = Money.parse(target.getText().toString());
                    if (n.isEmpty()) { name.setError("Isi nama"); return false; }
                    if (t <= 0) { target.setError("Isi target"); return false; }
                    cb.accept(n, t, (int) Money.parse(lock.getText().toString()), types[selType[0]], selBoss[0]);
                    return true;
                });
    }

    public static SqDialog.Builder emergencyConfirm(Context c, VaultEntity v, long amount, Runnable onConfirm) {
        int exp = PenaltySystem.emergencyExp(amount);
        String msg = PenaltySystem.motivation(new Random())
                + "\n\nPenalti: -" + exp + " EXP dan -" + PenaltySystem.EMERGENCY_HP + " HP."
                + "\nBrankas terkunci sampai " + DateUtil.formatDate(v.lockUntil) + ".";
        return new SqDialog.Builder(c)
                .icon(R.drawable.ic_alert)
                .accent(SqDialog.RED)
                .title("Tarik dana darurat?")
                .message(msg)
                .negative("Batalkan", null)
                .positive("Tetap tarik", () -> {
                    onConfirm.run();
                    return true;
                });
    }

    public static SqDialog.Builder report(Context c, ReportCallback cb) {
        final ReportExporter.Period[] periods = ReportExporter.Period.values();
        final int[] selPeriod = {1};
        final boolean[] pdf = {true};
        String[] names = new String[periods.length];
        for (int i = 0; i < periods.length; i++) names[i] = periods[i].label;
        LinearLayout periodRow = chipRow(c, names, 1, i -> selPeriod[0] = i);
        LinearLayout formatRow = chipRow(c, new String[]{"PDF", "CSV"}, 0, i -> pdf[0] = i == 0);
        return new SqDialog.Builder(c)
                .icon(R.drawable.ic_file)
                .accent(SqDialog.PURPLE)
                .title("Laporan keuangan")
                .message("Rekap tabungan dan riwayat transaksi.")
                .body(column(c, caption(c, "Periode"), periodRow, caption(c, "Format"), formatRow))
                .negative("Batal", null)
                .positive("Buat laporan", () -> {
                    cb.accept(periods[selPeriod[0]], pdf[0]);
                    return true;
                });
    }

    public static SqDialog.Builder sharePreview(Context c, Bitmap card, Runnable onShare) {
        ImageView iv = new ImageView(c);
        iv.setImageBitmap(card);
        iv.setAdjustViewBounds(true);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setMaxHeight(Ui.dp(c, 380));
        return new SqDialog.Builder(c)
                .icon(R.drawable.ic_share)
                .accent(SqDialog.PURPLE)
                .title("Kartu pencapaian")
                .message("Siap dibagikan ke Story atau Status.")
                .body(iv)
                .negative("Tutup", null)
                .positive("Bagikan", () -> {
                    onShare.run();
                    return true;
                });
    }

    public static SqDialog.Builder roundUp(Context c, VaultEntity v, RoundCallback cb) {
        EditText spent = field(c, "Nominal belanja (Rp)", true);
        EditText step = field(c, "Bulatkan ke kelipatan (Rp)", true);
        step.setText("1000");
        TextView preview = caption(c, "Kembalian yang disisihkan akan tampil di sini");
        preview.setTextColor(Ui.color(c, R.color.sq_green_2));
        preview.setTypeface(null, Typeface.BOLD);
        TextWatcher w = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) {}
            @Override
            public void afterTextChanged(Editable e) {
                long ch = RoundUp.change(Money.parse(spent.getText().toString()),
                        Money.parse(step.getText().toString()));
                preview.setText(ch > 0 ? "Disisihkan: " + Money.rp(ch) : "Tidak ada kembalian");
            }
        };
        spent.addTextChangedListener(w);
        step.addTextChangedListener(w);
        return new SqDialog.Builder(c)
                .icon(R.drawable.ic_coin)
                .accent(SqDialog.GOLD)
                .title("Pembulatan kembalian")
                .message("Tujuan: " + v.name)
                .body(column(c, spent, step, preview))
                .negative("Batal", null)
                .positive("Sisihkan", () -> {
                    cb.accept(Money.parse(spent.getText().toString()), Money.parse(step.getText().toString()));
                    return true;
                });
    }

    // ---------- menu vault ----------
    public static SqDialog.Builder vaultMenu(Context c, VaultEntity v, MenuListener l) {
        final SqDialog.Builder[] self = new SqDialog.Builder[1];
        int[] icons = {R.drawable.ic_deposit, R.drawable.ic_withdraw, R.drawable.ic_coin, R.drawable.ic_trash};
        String[] labels = {"Setor", "Tarik", "Pembulatan kembalian", "Hapus vault"};
        LinearLayout list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < labels.length; i++) {
            final int action = i;
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setClickable(true);
            row.setFocusable(true);
            row.setBackgroundResource(R.drawable.bg_btn_ghost);
            int p = Ui.dp(c, 14);
            row.setPadding(p, p, p, p);
            ImageView iv = new ImageView(c);
            iv.setImageResource(icons[i]);
            iv.setColorFilter(i == 3 ? Ui.color(c, R.color.sq_red_1) : Ui.color(c, R.color.sq_green_2));
            row.addView(iv, new LinearLayout.LayoutParams(Ui.dp(c, 22), Ui.dp(c, 22)));
            TextView t = new TextView(c);
            t.setText(labels[i]);
            t.setTextSize(15);
            t.setTextColor(Ui.color(c, i == 3 ? R.color.sq_red_1 : R.color.sq_text));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tp.setMarginStart(Ui.dp(c, 14));
            row.addView(t, tp);
            row.setOnClickListener(x -> {
                self[0].dismiss();
                l.onAction(action);
            });
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) rp.topMargin = Ui.dp(c, 8);
            list.addView(row, rp);
        }
        boolean locked = v.lockUntil > System.currentTimeMillis();
        self[0] = new SqDialog.Builder(c)
                .icon(locked ? R.drawable.ic_lock : R.drawable.ic_vault)
                .title(v.name)
                .message(locked ? "Terkunci sampai " + DateUtil.formatDate(v.lockUntil)
                        : Money.rp(v.savedAmount) + " dari " + Money.rp(v.targetAmount))
                .body(list)
                .positive("Tutup", null);
        return self[0];
    }

    public static SqDialog.Builder confirm(Context c, String title, String msg, int accent,
                                           String yes, Runnable onYes) {
        return new SqDialog.Builder(c)
                .icon(accent == SqDialog.RED ? R.drawable.ic_trash : R.drawable.ic_shield)
                .accent(accent)
                .title(title)
                .message(msg)
                .negative("Batal", null)
                .positive(yes, () -> {
                    onYes.run();
                    return true;
                });
    }

    // ---------- quest / reward / badge ----------
    public static SqDialog.Builder questReady(Context c, QuestEntity q, Runnable onClaim) {
        return new SqDialog.Builder(c)
                .icon(R.drawable.ic_target)
                .accent(SqDialog.GOLD)
                .title("Quest selesai!")
                .message(q.title + "\nHadiah: +" + q.rewardExp + " EXP")
                .negative("Nanti", null)
                .positive("Klaim +" + q.rewardExp + " EXP", () -> {
                    onClaim.run();
                    return true;
                });
    }

    public static SqDialog.Builder reward(Context c, int icon, int accent, String title,
                                          String message, String button) {
        return new SqDialog.Builder(c).icon(icon).accent(accent).title(title)
                .message(message).positive(button, null);
    }

    public static SqDialog.Builder levelUp(Context c, int level) {
        return reward(c, R.drawable.ic_bolt, SqDialog.GOLD, "Naik level!",
                "Kamu sekarang Level " + level + ". Terus menabung!", "Mantap");
    }

    public static SqDialog.Builder badgeUnlock(Context c, BadgeCatalog.Def d) {
        LinearLayout body = new LinearLayout(c);
        body.setGravity(Gravity.CENTER);
        ImageView tile = new ImageView(c);
        int[] col = Ui.tier(d.tier);
        tile.setBackground(Ui.oval(col[0], col[1]));
        int pad = Ui.dp(c, 22);
        tile.setPadding(pad, pad, pad, pad);
        tile.setImageResource(d.iconRes);
        tile.setElevation(Ui.dp(c, 8));
        body.addView(tile, new LinearLayout.LayoutParams(Ui.dp(c, 96), Ui.dp(c, 96)));
        return new SqDialog.Builder(c)
                .icon(R.drawable.ic_trophy)
                .accent(SqDialog.PURPLE)
                .title("Badge terbuka!")
                .message(d.title + "\n" + d.description)
                .body(body)
                .positive("Keren", null);
    }

    public static SqDialog.Builder badgeInfo(Context c, BadgeCatalog.Def d, boolean unlocked,
                                             long unlockedAt, String progressText) {
        boolean hidden = d.secret && !unlocked;
        LinearLayout body = new LinearLayout(c);
        body.setGravity(Gravity.CENTER);
        ImageView tile = new ImageView(c);
        int[] col = unlocked ? Ui.tier(d.tier)
                : new int[]{Ui.color(c, R.color.sq_locked), Ui.color(c, R.color.sq_locked)};
        tile.setBackground(Ui.oval(col[0], col[1]));
        int pad = Ui.dp(c, 22);
        tile.setPadding(pad, pad, pad, pad);
        tile.setImageResource(hidden ? R.drawable.ic_lock : d.iconRes);
        if (!unlocked) tile.setColorFilter(Ui.color(c, R.color.sq_text_dim));
        body.addView(tile, new LinearLayout.LayoutParams(Ui.dp(c, 96), Ui.dp(c, 96)));

        String status;
        if (unlocked) status = "Terbuka pada " + DateUtil.formatDate(unlockedAt);
        else if (progressText != null) status = "Progres: " + progressText;
        else status = "Masih terkunci";

        String tier = hidden ? "" : "Tier " + BadgeCatalog.tierName(d.tier) + "  |  " + d.category + "\n";
        String text = hidden
                ? "Badge rahasia. Terbuka lewat kejadian khusus (waktu, tanggal, atau situasi tertentu)."
                : tier + d.description;

        return new SqDialog.Builder(c)
                .icon(R.drawable.ic_trophy)
                .accent(unlocked ? SqDialog.PURPLE : SqDialog.GREEN)
                .title(hidden ? "Badge rahasia" : d.title)
                .message(text + "\n" + status)
                .body(body)
                .positive("Tutup", null);
    }

    /** Dialog beli item habis-pakai dengan pilihan jumlah. */
    public static SqDialog.Builder buyItem(Context c, ShopItemEntity it, int owned, long coins, QtyCallback cb) {
        final int[] qtys = {1, 3, 5, 10};
        final int[] sel = {1};
        final TextView total = caption(c, "");
        total.setTextSize(13);
        total.setTypeface(null, Typeface.BOLD);
        total.setTextColor(Ui.color(c, R.color.sq_text));
        final Runnable update = () -> total.setText("Total: " + ((long) it.price * sel[0]) + " koin   |   Saldo: " + coins + " koin");
        update.run();
        LinearLayout row = chipRow(c, new String[]{"x1", "x3", "x5", "x10"}, 0, i -> {
            sel[0] = qtys[i];
            update.run();
        });
        return new SqDialog.Builder(c)
                .icon(ItemIcons.res(it.iconKey))
                .accent(it.rarity >= 3 ? SqDialog.GOLD : SqDialog.GREEN)
                .title("Beli " + it.name)
                .message(it.description + "\nKamu punya: " + owned)
                .body(column(c, caption(c, "Jumlah"), row, total))
                .negative("Batal", null)
                .positive("Beli", () -> {
                    if (coins < (long) it.price * sel[0]) {
                        total.setText("Koin tidak cukup untuk jumlah ini");
                        return false;
                    }
                    cb.accept(sel[0]);
                    return true;
                });
    }

    /** Dialog pengaturan sumber update (repo GitHub atau URL JSON). */
    public static SqDialog.Builder updateSource(Context c, String current, TextCallback cb) {
        EditText input = field(c, "pemilik/repo  (mis. budi/B)", false);
        input.setText(current);
        input.setSelection(input.getText().length());
        TextView help = caption(c, "Isi nama repo GitHub tempat APK dirilis (harus publik), atau URL JSON update kustom. "
                + "Kosongkan untuk mematikan pemeriksaan.");
        return new SqDialog.Builder(c)
                .icon(R.drawable.ic_download)
                .accent(SqDialog.PURPLE)
                .title("Sumber pembaruan")
                .body(column(c, input, help))
                .negative("Batal", null)
                .positive("Simpan", () -> {
                    cb.accept(input.getText().toString().trim());
                    return true;
                });
    }

    // ---------- simulator ----------
    public static SqDialog.Builder simulator(Context c, List<VaultEntity> vaults) {
        EditText target = field(c, "Target (Rp)", true);
        EditText saved = field(c, "Sudah terkumpul (Rp)", true);
        EditText per = field(c, "Setoran per periode (Rp)", true);

        final ProjectionCalculator.Frequency[] freqs = ProjectionCalculator.Frequency.values();
        final int[] selected = {0};
        final TextView[] freqChips = new TextView[freqs.length];
        LinearLayout freqRow = new LinearLayout(c);
        String[] freqNames = {"Harian", "Mingguan", "Bulanan"};
        for (int i = 0; i < freqs.length; i++) {
            final int idx = i;
            TextView ch = chip(c, freqNames[i]);
            freqChips[i] = ch;
            Ui.styleChip(ch, i == 0);
            ch.setOnClickListener(x -> {
                selected[0] = idx;
                for (int k = 0; k < freqChips.length; k++) Ui.styleChip(freqChips[k], k == idx);
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) lp.setMarginStart(Ui.dp(c, 8));
            freqRow.addView(ch, lp);
        }

        TextView result = new TextView(c);
        result.setTextSize(14);
        result.setTextColor(Ui.color(c, R.color.sq_text));
        result.setText("Isi data lalu tekan Hitung.");

        View vaultRow = null;
        if (vaults != null && !vaults.isEmpty()) {
            LinearLayout chipsRow = new LinearLayout(c);
            for (VaultEntity v : vaults) {
                TextView ch = chip(c, v.name);
                ch.setOnClickListener(x -> {
                    target.setText(String.valueOf(v.targetAmount));
                    saved.setText(String.valueOf(v.savedAmount));
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMarginEnd(Ui.dp(c, 8));
                chipsRow.addView(ch, lp);
            }
            HorizontalScrollView hs = new HorizontalScrollView(c);
            hs.setHorizontalScrollBarEnabled(false);
            hs.addView(chipsRow);
            vaultRow = column(c, caption(c, "Isi otomatis dari vault:"), hs);
        }

        LinearLayout body = vaultRow == null
                ? column(c, target, saved, per, freqRow, result)
                : column(c, vaultRow, target, saved, per, freqRow, result);

        return new SqDialog.Builder(c)
                .icon(R.drawable.ic_trend)
                .accent(SqDialog.PURPLE)
                .title("Simulator tabungan")
                .message("Perkirakan kapan targetmu tercapai.")
                .body(body)
                .negative("Tutup", null)
                .positive("Hitung", () -> {
                    long t = Money.parse(target.getText().toString());
                    long s = Money.parse(saved.getText().toString());
                    long p = Money.parse(per.getText().toString());
                    if (t <= 0 || p <= 0) {
                        result.setText("Isi target dan setoran per periode dulu.");
                        return false;
                    }
                    long n = ProjectionCalculator.periodsNeeded(t, s, p);
                    if (n == 0) {
                        result.setText("Target sudah tercapai!");
                    } else {
                        ProjectionCalculator.Frequency f = freqs[selected[0]];
                        long days = n * f.days;
                        long eta = System.currentTimeMillis() + days * 86_400_000L;
                        result.setText(String.format(Locale.US, "Butuh %d %s (sekitar %d hari)\nEstimasi tercapai: %s\nSisa target: %s",
                                n, f.label, days, DateUtil.formatDate(eta), Money.rp(t - s)));
                    }
                    return false;
                });
    }

    // ---------- pengaturan ----------
    public static SqDialog.Builder timePicker(Context c, int hour, int minute, TimeCallback cb) {
        NumberPicker nh = new NumberPicker(c);
        nh.setMinValue(0);
        nh.setMaxValue(23);
        nh.setValue(hour);
        nh.setFormatter(v -> String.format(Locale.US, "%02d", v));
        NumberPicker nm = new NumberPicker(c);
        nm.setMinValue(0);
        nm.setMaxValue(59);
        nm.setValue(minute);
        nm.setFormatter(v -> String.format(Locale.US, "%02d", v));
        TextView colon = new TextView(c);
        colon.setText(":");
        colon.setTextSize(24);
        colon.setTextColor(Ui.color(c, R.color.sq_text));
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER);
        row.addView(nh);
        row.addView(colon);
        row.addView(nm);
        return new SqDialog.Builder(c)
                .icon(R.drawable.ic_bell)
                .title("Waktu pengingat")
                .body(row)
                .negative("Batal", null)
                .positive("Simpan", () -> {
                    cb.accept(nh.getValue(), nm.getValue());
                    return true;
                });
    }

    public static SqDialog.Builder changePin(Context c, PinStore store, MessageCallback cb) {
        EditText cur = pinField(c, "PIN saat ini");
        EditText n1 = pinField(c, "PIN baru (6 digit)");
        EditText n2 = pinField(c, "Ulangi PIN baru");
        return new SqDialog.Builder(c)
                .icon(R.drawable.ic_lock)
                .title("Ubah PIN")
                .body(column(c, cur, n1, n2))
                .negative("Batal", null)
                .positive("Simpan", () -> {
                    String a = cur.getText().toString();
                    String b = n1.getText().toString();
                    String d = n2.getText().toString();
                    if (!store.verify(a)) { cur.setError("PIN salah"); return false; }
                    if (b.length() != PinStore.LENGTH) { n1.setError("Harus 6 digit"); return false; }
                    if (!b.equals(d)) { n2.setError("PIN tidak sama"); return false; }
                    store.setPin(b);
                    cb.show("PIN berhasil diubah");
                    return true;
                });
    }
}
