package com.savequest.app.report;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.savequest.app.data.local.AppDatabase;
import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.data.local.entity.TransactionEntity;
import com.savequest.app.data.local.entity.TxWithVault;
import com.savequest.app.data.local.entity.VaultEntity;
import com.savequest.app.domain.LevelSystem;
import com.savequest.app.domain.StreakSystem;
import com.savequest.app.util.DateUtil;
import com.savequest.app.util.Money;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Laporan tabungan harian/bulanan/semua dalam format PDF (PdfDocument) atau CSV. */
public final class ReportExporter {
    private ReportExporter() {}

    public enum Period {
        TODAY("Hari ini"), THIS_MONTH("Bulan ini"), LAST_MONTH("Bulan lalu"), ALL("Semua");

        public final String label;
        Period(String label) { this.label = label; }
    }

    public interface Callback {
        void onDone(boolean ok, String message);
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static String fileName(Period p, boolean pdf) {
        return "laporan-savequest-" + new SimpleDateFormat("yyyyMMdd", Locale.US).format(new Date())
                + "-" + p.name().toLowerCase(Locale.US) + (pdf ? ".pdf" : ".csv");
    }

    public static void export(Context ctx, Uri uri, boolean pdf, Period period, Callback cb) {
        final Context app = ctx.getApplicationContext();
        IO.execute(() -> {
            try {
                AppDatabase db = AppDatabase.get(app);
                long[] range = range(period);
                List<TxWithVault> rows = db.transactionDao().rangeSync(range[0], range[1]);
                try (OutputStream os = app.getContentResolver().openOutputStream(uri, "wt")) {
                    if (os == null) throw new IllegalStateException("File tujuan tidak bisa dibuka");
                    if (pdf) {
                        writePdf(os, db, rows, period);
                    } else {
                        os.write(toCsv(rows).getBytes(StandardCharsets.UTF_8));
                    }
                }
                done(cb, true, "Laporan " + (pdf ? "PDF" : "CSV") + " tersimpan (" + rows.size() + " transaksi)");
            } catch (Exception e) {
                done(cb, false, "Export laporan gagal: " + e.getMessage());
            }
        });
    }

    private static void done(Callback cb, boolean ok, String msg) {
        if (cb != null) MAIN.post(() -> cb.onDone(ok, msg));
    }

    /** Rentang epoch day [from, to] untuk periode. */
    static long[] range(Period p) {
        long today = DateUtil.today();
        switch (p) {
            case TODAY: return new long[]{today, today};
            case THIS_MONTH: return new long[]{DateUtil.monthStart(), DateUtil.monthEnd()};
            case LAST_MONTH:
                LocalDate first = LocalDate.now().withDayOfMonth(1).minusMonths(1);
                return new long[]{first.toEpochDay(), first.withDayOfMonth(first.lengthOfMonth()).toEpochDay()};
            default: return new long[]{0, today + 1};
        }
    }

    // ---------- CSV ----------
    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    static String toCsv(List<TxWithVault> rows) {
        SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat time = new SimpleDateFormat("HH:mm", Locale.US);
        StringBuilder sb = new StringBuilder("\uFEFF"); // BOM supaya Excel membaca UTF-8
        sb.append("Tanggal,Jam,Vault,Jenis,Nominal,Catatan,EXP\n");
        for (TxWithVault r : rows) {
            Date d = new Date(r.tx.createdAt);
            sb.append(date.format(d)).append(',').append(time.format(d)).append(',')
                    .append(csv(r.vaultName)).append(',').append(r.tx.type).append(',')
                    .append(r.tx.amount).append(',').append(csv(r.tx.note)).append(',')
                    .append(r.tx.expEarned).append('\n');
        }
        return sb.toString();
    }

    // ---------- PDF ----------
    private static final class Pdf {
        final PdfDocument doc = new PdfDocument();
        PdfDocument.Page page;
        Canvas c;
        int number = 0;
        float y;
        final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        final Paint bold = new Paint(Paint.ANTI_ALIAS_FLAG);

        Pdf() {
            body.setTextSize(9.5f);
            body.setColor(0xFF1B2430);
            bold.setTextSize(9.5f);
            bold.setTypeface(Typeface.DEFAULT_BOLD);
            bold.setColor(0xFF1B2430);
            newPage();
        }

        void newPage() {
            if (page != null) doc.finishPage(page);
            number++;
            page = doc.startPage(new PdfDocument.PageInfo.Builder(595, 842, number).create());
            c = page.getCanvas();
            y = 48;
        }

        void ensure(float h) {
            if (y + h > 800) newPage();
        }

        static String fit(String s, Paint p, float w) {
            if (s == null) return "";
            if (p.measureText(s) <= w) return s;
            int n = p.breakText(s, true, w - p.measureText("..."), null);
            return s.substring(0, Math.max(0, n)) + "...";
        }
    }

    private static void writePdf(OutputStream os, AppDatabase db, List<TxWithVault> rows, Period period) throws Exception {
        Pdf pdf = new Pdf();
        PlayerEntity player = db.playerDao().get();
        List<VaultEntity> vaults = db.vaultDao().allSync();
        SimpleDateFormat full = new SimpleDateFormat("dd MMM yyyy HH:mm", new Locale("id", "ID"));

        long in = 0, out = 0;
        for (TxWithVault r : rows) {
            if (r.tx.amount > 0) in += r.tx.amount; else out += -r.tx.amount;
        }

        // judul
        pdf.bold.setTextSize(20);
        pdf.c.drawText("Laporan Tabungan SaveQuest", 36, pdf.y, pdf.bold);
        pdf.bold.setTextSize(9.5f);
        pdf.y += 18;
        pdf.c.drawText("Periode: " + period.label + "   |   Dibuat: " + full.format(new Date()), 36, pdf.y, pdf.body);
        pdf.y += 26;

        // ringkasan
        pdf.bold.setTextSize(12);
        pdf.c.drawText("Ringkasan", 36, pdf.y, pdf.bold);
        pdf.bold.setTextSize(9.5f);
        pdf.y += 16;
        String[][] summary = {
                {"Total setoran", Money.rp(in)},
                {"Total penarikan", Money.rp(out)},
                {"Bersih", Money.rp(in - out)},
                {"Jumlah transaksi", String.valueOf(rows.size())},
        };
        for (String[] s : summary) {
            pdf.c.drawText(s[0], 44, pdf.y, pdf.body);
            pdf.c.drawText(s[1], 200, pdf.y, pdf.bold);
            pdf.y += 14;
        }
        if (player != null) {
            int level = LevelSystem.levelFor(player.totalExp);
            int streak = StreakSystem.effective(player.streak, player.lastDepositDay, DateUtil.today());
            pdf.c.drawText("Level / EXP", 44, pdf.y, pdf.body);
            pdf.c.drawText("Lv " + level + " (" + LevelSystem.title(level) + ") / " + player.totalExp + " EXP", 200, pdf.y, pdf.bold);
            pdf.y += 14;
            pdf.c.drawText("Streak / HP", 44, pdf.y, pdf.body);
            pdf.c.drawText(streak + " hari / " + player.hp + " HP", 200, pdf.y, pdf.bold);
            pdf.y += 14;
        }
        pdf.y += 14;

        // tabel vault
        pdf.bold.setTextSize(12);
        pdf.c.drawText("Posisi Vault", 36, pdf.y, pdf.bold);
        pdf.bold.setTextSize(9.5f);
        pdf.y += 16;
        pdf.c.drawText("Nama", 44, pdf.y, pdf.bold);
        pdf.c.drawText("Jenis", 220, pdf.y, pdf.bold);
        pdf.c.drawText("Saldo", 300, pdf.y, pdf.bold);
        pdf.c.drawText("Target", 400, pdf.y, pdf.bold);
        pdf.c.drawText("%", 520, pdf.y, pdf.bold);
        pdf.y += 14;
        for (VaultEntity v : vaults) {
            pdf.ensure(14);
            int pct = v.targetAmount > 0 ? (int) Math.min(100, v.savedAmount * 100 / v.targetAmount) : 0;
            pdf.c.drawText(Pdf.fit(v.name, pdf.body, 165), 44, pdf.y, pdf.body);
            pdf.c.drawText(v.type == null ? "NORMAL" : v.type, 220, pdf.y, pdf.body);
            pdf.c.drawText(Money.rp(v.savedAmount), 300, pdf.y, pdf.body);
            pdf.c.drawText(Money.rp(v.targetAmount), 400, pdf.y, pdf.body);
            pdf.c.drawText(pct + "%", 520, pdf.y, pdf.body);
            pdf.y += 14;
        }
        pdf.y += 16;

        // tabel transaksi
        pdf.ensure(40);
        pdf.bold.setTextSize(12);
        pdf.c.drawText("Riwayat Transaksi", 36, pdf.y, pdf.bold);
        pdf.bold.setTextSize(9.5f);
        pdf.y += 16;
        header(pdf);
        SimpleDateFormat shortDate = new SimpleDateFormat("dd/MM HH:mm", Locale.US);
        for (TxWithVault r : rows) {
            if (pdf.y + 14 > 800) {
                pdf.newPage();
                header(pdf);
            }
            TransactionEntity t = r.tx;
            pdf.c.drawText(shortDate.format(new Date(t.createdAt)), 36, pdf.y, pdf.body);
            pdf.c.drawText(Pdf.fit(r.vaultName, pdf.body, 105), 100, pdf.y, pdf.body);
            pdf.c.drawText(t.type, 212, pdf.y, pdf.body);
            String amount = (t.amount >= 0 ? "+" : "-") + Money.rp(Math.abs(t.amount));
            pdf.c.drawText(amount, 330 - pdf.body.measureText(amount) + 70, pdf.y, pdf.body);
            pdf.c.drawText(Pdf.fit(t.note, pdf.body, 160), 410, pdf.y, pdf.body);
            pdf.y += 14;
        }
        if (rows.isEmpty()) pdf.c.drawText("Tidak ada transaksi pada periode ini.", 36, pdf.y, pdf.body);

        pdf.doc.finishPage(pdf.page);
        pdf.doc.writeTo(os);
        pdf.doc.close();
    }

    private static void header(Pdf pdf) {
        pdf.c.drawText("Waktu", 36, pdf.y, pdf.bold);
        pdf.c.drawText("Vault", 100, pdf.y, pdf.bold);
        pdf.c.drawText("Jenis", 212, pdf.y, pdf.bold);
        pdf.c.drawText("Nominal", 340, pdf.y, pdf.bold);
        pdf.c.drawText("Catatan", 410, pdf.y, pdf.bold);
        pdf.y += 14;
    }
}
