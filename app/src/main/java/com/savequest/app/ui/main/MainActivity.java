package com.savequest.app.ui.main;

import android.os.Bundle;

import androidx.lifecycle.ViewModelProvider;

import com.savequest.app.R;
import com.savequest.app.feedback.FeedbackManager;
import com.savequest.app.ui.nav.FloatingNavBar;
import com.savequest.app.ui.settings.UpdateFlow;
import com.savequest.app.update.UpdateChecker;
import com.savequest.app.util.AppPrefs;
import com.savequest.app.util.Session;

public class MainActivity extends SecureActivity {
    private static final String KEY_TAB = "tab";
    private static final long CHECK_INTERVAL = 12L * 60 * 60 * 1000;

    private MainViewModel vm;
    private FloatingNavBar nav;
    private ScreenManager screens;
    private ResultPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final FeedbackManager fx = FeedbackManager.get(this);
        vm = new ViewModelProvider(this).get(MainViewModel.class);
        presenter = new ResultPresenter(this, vm);
        nav = findViewById(R.id.navBar);
        screens = new ScreenManager(getSupportFragmentManager(), R.id.container);

        int tab = savedInstanceState != null ? savedInstanceState.getInt(KEY_TAB, 1) : 1;
        nav.select(tab, false);
        if (savedInstanceState == null) screens.show(tab, false);
        else screens.setCurrent(tab);

        nav.setListener(index -> {
            fx.tick();
            screens.show(index, true);
        });

        vm.result.observe(this, r -> {
            if (r == null) return;
            vm.result.setValue(null);
            presenter.present(r);
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt(KEY_TAB, nav.getSelected());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!Session.unlocked) return;
        vm.refresh();
        autoCheckUpdate();
    }

    /** Cek pembaruan otomatis (maksimal tiap 12 jam) jika sumber update sudah diatur. */
    private void autoCheckUpdate() {
        AppPrefs prefs = new AppPrefs(this);
        if (!prefs.updateAuto() || prefs.updateSource().isEmpty()) return;
        if (System.currentTimeMillis() - prefs.lastUpdateCheck() < CHECK_INTERVAL) return;
        UpdateChecker.check(this, res -> {
            if (res.status == UpdateChecker.Result.UPDATE && !isFinishing() && !isDestroyed()) {
                presenter.enqueue(UpdateFlow.promptBuilder(this, res.info));
            }
        });
    }
}
