package com.savequest.app.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.savequest.app.R;
import com.savequest.app.ui.common.SqDialog;
import com.savequest.app.ui.common.Ui;
import com.savequest.app.update.UpdateChecker;
import com.savequest.app.update.UpdateInstaller;
import com.savequest.app.util.Session;

/** Alur UI pembaruan: dialog info, izin "pasang dari sumber ini", progres unduh, lalu pasang. */
public final class UpdateFlow {
    private UpdateFlow() {}

    public static SqDialog.Builder promptBuilder(Activity act, UpdateChecker.Info info) {
        String notes = info.notes == null || info.notes.isEmpty() ? "" : "\n\n" + info.notes;
        return new SqDialog.Builder(act)
                .icon(R.drawable.ic_download)
                .accent(SqDialog.PURPLE)
                .title("Pembaruan tersedia")
                .message("SaveQuest " + info.versionName + " siap dipasang." + notes
                        + "\n\nData kamu (vault, level, EXP, streak, inventory) tetap aman. "
                        + "Aplikasi akan menimpa versi lama tanpa uninstall.")
                .negative("Nanti", null)
                .positive("Perbarui sekarang", () -> {
                    run(act, info);
                    return true;
                });
    }

    public static void prompt(Activity act, UpdateChecker.Info info) {
        promptBuilder(act, info).show();
    }

    public static void run(Activity act, UpdateChecker.Info info) {
        if (!act.getPackageManager().canRequestPackageInstalls()) {
            new SqDialog.Builder(act)
                    .icon(R.drawable.ic_shield)
                    .title("Izin pemasangan")
                    .message("Agar bisa memperbarui dirinya sendiri, izinkan SaveQuest memasang aplikasi. "
                            + "Aktifkan 'Izinkan dari sumber ini', lalu kembali dan tekan Perbarui lagi.")
                    .negative("Batal", null)
                    .positive("Buka pengaturan", () -> {
                        Session.markExternal();
                        act.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + act.getPackageName())));
                        return true;
                    }).show();
            return;
        }

        final ProgressBar bar = new ProgressBar(act, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        final TextView status = new TextView(act);
        status.setTextSize(13);
        status.setTypeface(null, Typeface.BOLD);
        status.setTextColor(Ui.color(act, R.color.sq_text));
        status.setText("Menyiapkan...");
        LinearLayout body = new LinearLayout(act);
        body.setOrientation(LinearLayout.VERTICAL);
        body.addView(status);
        body.addView(bar, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(act, 18)));

        final SqDialog.Builder dlg = new SqDialog.Builder(act)
                .icon(R.drawable.ic_download)
                .title("Memperbarui SaveQuest")
                .body(body)
                .cancelable(false)
                .positive("Mohon tunggu", () -> false);
        dlg.show();

        UpdateInstaller.downloadAndInstall(act.getApplicationContext(), info, new UpdateInstaller.Progress() {
            @Override public void onStage(String text) { status.setText(text); }
            @Override public void onProgress(int percent) { bar.setProgress(percent); }
            @Override public void onDone(boolean ok, String message) {
                dlg.dismiss();
                Toast.makeText(act.getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
