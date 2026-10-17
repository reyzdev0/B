package com.savequest.app.ui.settings;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.biometric.BiometricManager;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.savequest.app.R;
import com.savequest.app.audio.BgmPlayer;
import com.savequest.app.data.backup.BackupManager;
import com.savequest.app.feedback.FeedbackManager;
import com.savequest.app.reminder.ReminderScheduler;
import com.savequest.app.report.ReportExporter;
import com.savequest.app.ui.common.Ui;
import com.savequest.app.ui.common.DialogFactory;
import com.savequest.app.ui.common.SqDialog;
import com.savequest.app.ui.main.MainViewModel;
import com.savequest.app.update.UpdateChecker;
import com.savequest.app.util.AppPrefs;
import com.savequest.app.util.DateUtil;
import com.savequest.app.util.PinStore;
import com.savequest.app.util.Session;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** 5 fungsi: Backup/Restore, Reminder, Simulator, Keamanan, Sound & Haptic. */
public class SettingsFragment extends Fragment {
    private AppPrefs prefs;
    private FeedbackManager fx;
    private MainViewModel vm;
    private SwitchCompat swReminder;
    private TextView tvReminderTime;
    private boolean binding = false;
    private ReportExporter.Period pendingPeriod = ReportExporter.Period.THIS_MONTH;

    private final ActivityResultLauncher<String> pdfLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/pdf"), uri -> {
                if (uri != null) doReport(uri, true);
            });

    private final ActivityResultLauncher<String> csvLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/csv"), uri -> {
                if (uri != null) doReport(uri, false);
            });

    /** Membuka picker/dialog sistem tanpa memicu kunci ulang aplikasi. */
    private <T> void launchExternal(ActivityResultLauncher<T> launcher, T input) {
        Session.markExternal();
        launcher.launch(input);
    }

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri != null) doExport(uri);
            });

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) confirmImport(uri);
            });

    private final ActivityResultLauncher<String> notifPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (!granted) {
                    toast("Izin notifikasi ditolak, pengingat tidak akan tampil");
                    setReminder(false);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle state) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle state) {
        super.onViewCreated(v, state);
        Context c = requireContext();
        prefs = new AppPrefs(c);
        fx = FeedbackManager.get(c);
        vm = new ViewModelProvider(requireActivity()).get(MainViewModel.class);

        bindBackup(v);
        bindReminder(v);
        bindSimulator(v);
        bindSecurity(v);
        bindFx(v);
        bindReport(v);
        bindBgm(v);
        bindUpdate(v);
    }

    // ---------- 1. Backup & Restore ----------
    private void bindBackup(View v) {
        v.findViewById(R.id.btnExport).setOnClickListener(x -> launchExternal(exportLauncher, 
                "savequest-backup-" + new SimpleDateFormat("yyyyMMdd-HHmm", Locale.US).format(new Date()) + ".json"));
        v.findViewById(R.id.btnImport).setOnClickListener(x ->
                launchExternal(importLauncher, new String[]{"*/*"}));
    }

    private void doExport(Uri uri) {
        BackupManager.export(requireContext(), uri, (ok, msg) -> toast(msg));
    }

    private void confirmImport(Uri uri) {
        DialogFactory.confirm(requireContext(), "Pulihkan data?",
                "Seluruh data saat ini akan diganti dengan isi file backup.",
                SqDialog.RED, "Pulihkan", () ->
                        BackupManager.restore(requireContext(), uri, (ok, msg) -> toast(msg))).show();
    }

    // ---------- 2. Smart Daily Reminder ----------
    private void bindReminder(View v) {
        swReminder = v.findViewById(R.id.swReminder);
        tvReminderTime = v.findViewById(R.id.tvReminderTime);
        tvReminderTime.setText(DateUtil.time(prefs.reminderHour(), prefs.reminderMinute()));
        binding = true;
        swReminder.setChecked(prefs.reminderEnabled());
        binding = false;

        swReminder.setOnCheckedChangeListener((b, checked) -> {
            if (binding) return;
            prefs.setReminder(checked);
            ReminderScheduler.schedule(requireContext());
            if (checked && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                launchExternal(notifPermission, Manifest.permission.POST_NOTIFICATIONS);
            }
            if (checked) toast("Pengingat aktif pukul " + DateUtil.time(prefs.reminderHour(), prefs.reminderMinute()));
        });

        v.findViewById(R.id.rowReminderTime).setOnClickListener(x ->
                DialogFactory.timePicker(requireContext(), prefs.reminderHour(), prefs.reminderMinute(), (h, m) -> {
                    prefs.setReminderTime(h, m);
                    tvReminderTime.setText(DateUtil.time(h, m));
                    ReminderScheduler.schedule(requireContext());
                }).show());
    }

    private void setReminder(boolean on) {
        prefs.setReminder(on);
        ReminderScheduler.schedule(requireContext());
        if (swReminder != null) {
            binding = true;
            swReminder.setChecked(on);
            binding = false;
        }
    }

    // ---------- 3. Financial Simulator ----------
    private void bindSimulator(View v) {
        v.findViewById(R.id.btnSimulator).setOnClickListener(x ->
                DialogFactory.simulator(requireContext(), vm.vaults.getValue()).show());
    }

    // ---------- 4. Biometrik & PIN ----------
    private void bindSecurity(View v) {
        SwitchCompat sw = v.findViewById(R.id.swBiometric);
        sw.setChecked(prefs.biometricEnabled());
        sw.setOnCheckedChangeListener((b, checked) -> {
            if (checked) {
                int can = BiometricManager.from(requireContext())
                        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
                if (can != BiometricManager.BIOMETRIC_SUCCESS) {
                    toast("Biometrik tidak tersedia atau belum didaftarkan di HP");
                    sw.setChecked(false);
                    return;
                }
            }
            prefs.setBiometric(checked);
        });
        v.findViewById(R.id.btnChangePin).setOnClickListener(x ->
                DialogFactory.changePin(requireContext(), new PinStore(requireContext()), this::toast).show());
    }

    // ---------- 5. Sound & Haptic ----------
    private void bindFx(View v) {
        SwitchCompat swSound = v.findViewById(R.id.swSound);
        SwitchCompat swHaptic = v.findViewById(R.id.swHaptic);
        SeekBar seek = v.findViewById(R.id.seekHaptic);
        swSound.setChecked(prefs.soundEnabled());
        swHaptic.setChecked(prefs.hapticEnabled());
        seek.setProgress(prefs.hapticLevel() - 1);
        seek.setEnabled(prefs.hapticEnabled());

        swSound.setOnCheckedChangeListener((b, on) -> prefs.setSound(on));
        swHaptic.setOnCheckedChangeListener((b, on) -> {
            prefs.setHaptic(on);
            seek.setEnabled(on);
        });
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) prefs.setHapticLevel(p + 1);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) { fx.success(); }
        });
        v.findViewById(R.id.btnTestFx).setOnClickListener(x -> fx.claim());
    }

    // ---------- 6. Laporan keuangan (PDF / CSV) ----------
    private void bindReport(View v) {
        v.findViewById(R.id.btnReport).setOnClickListener(x ->
                DialogFactory.report(requireContext(), (period, pdf) -> {
                    pendingPeriod = period;
                    launchExternal(pdf ? pdfLauncher : csvLauncher, ReportExporter.fileName(period, pdf));
                }).show());
    }

    private void doReport(Uri uri, boolean pdf) {
        ReportExporter.export(requireContext(), uri, pdf, pendingPeriod, (ok, msg) -> toast(msg));
    }

    // ---------- 8. Pembaruan aplikasi (OTA) ----------
    private void bindUpdate(View v) {
        final Context c = requireContext();
        final TextView tvSource = v.findViewById(R.id.tvUpdateSource);
        final TextView tvStatus = v.findViewById(R.id.tvUpdateStatus);
        String name = UpdateChecker.installedVersionName(c);
        ((TextView) v.findViewById(R.id.tvUpdateVersion)).setText(
                "Versi terpasang: " + name + " (kode " + UpdateChecker.installedVersionCode(c) + ")");
        ((TextView) v.findViewById(R.id.tvVersion)).setText("SaveQuest v" + name);
        showSource(tvSource);

        v.findViewById(R.id.rowUpdateSource).setOnClickListener(x ->
                DialogFactory.updateSource(c, prefs.updateSource(), text -> {
                    prefs.setUpdateSource(text);
                    showSource(tvSource);
                }).show());

        SwitchCompat sw = v.findViewById(R.id.swUpdateAuto);
        sw.setChecked(prefs.updateAuto());
        sw.setOnCheckedChangeListener((b, on) -> prefs.setUpdateAuto(on));

        v.findViewById(R.id.btnCheckUpdate).setOnClickListener(x -> {
            tvStatus.setText("Memeriksa pembaruan...");
            UpdateChecker.check(c, res -> {
                if (!isAdded()) return;
                if (res.status == UpdateChecker.Result.UPDATE) {
                    tvStatus.setText("Versi " + res.info.versionName + " tersedia");
                    UpdateFlow.prompt(requireActivity(), res.info);
                } else {
                    tvStatus.setText(res.message);
                }
            });
        });

        v.findViewById(R.id.btnRestoreSnapshot).setOnClickListener(x ->
                DialogFactory.confirm(c, "Pulihkan snapshot?",
                        "Data saat ini diganti dengan snapshot pengaman terakhir yang dibuat otomatis sebelum update.",
                        SqDialog.RED, "Pulihkan",
                        () -> BackupManager.restoreLatestSnapshot(c, (ok, msg) -> toast(msg))).show());
    }

    private void showSource(TextView tv) {
        String s = prefs.updateSource();
        tv.setText(s.isEmpty() ? "Belum diatur" : s);
    }

    // ---------- 7. Musik latar ----------
    private void bindBgm(View v) {
        final BgmPlayer bgm = BgmPlayer.get(requireContext());
        SwitchCompat sw = v.findViewById(R.id.swBgm);
        SeekBar seek = v.findViewById(R.id.seekBgm);
        final TextView[] chips = {v.findViewById(R.id.chipTrack0), v.findViewById(R.id.chipTrack1), v.findViewById(R.id.chipTrack2)};
        sw.setChecked(bgm.isEnabled());
        seek.setProgress(prefs.bgmVolume());
        for (int i = 0; i < chips.length; i++) {
            final int idx = i;
            Ui.styleChip(chips[i], prefs.bgmTrack() == i);
            chips[i].setOnClickListener(x -> {
                for (int k = 0; k < chips.length; k++) Ui.styleChip(chips[k], k == idx);
                bgm.setTrack(idx);
            });
        }
        sw.setOnCheckedChangeListener((b, on) -> bgm.setEnabled(on));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                if (fromUser) bgm.setVolume(p);
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    private void toast(String msg) {
        Context c = getContext();
        if (c != null) Toast.makeText(c.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
