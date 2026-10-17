package com.savequest.app.ui.lock;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.savequest.app.R;
import com.savequest.app.feedback.FeedbackManager;
import com.savequest.app.ui.common.Ui;
import com.savequest.app.ui.main.MainActivity;
import com.savequest.app.util.AppPrefs;
import com.savequest.app.util.PinStore;
import com.savequest.app.util.Session;

/** Layar kunci: PIN 6 digit (titik animasi + keypad glass) dan tombol biometrik cepat. */
public class LockActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final StringBuilder entry = new StringBuilder();

    private PinStore pinStore;
    private AppPrefs prefs;
    private FeedbackManager fx;
    private PinDotsView dots;
    private TextView title, sub;
    private boolean setupMode, busy;
    private String firstEntry;
    private BiometricPrompt prompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_lock);

        pinStore = new PinStore(this);
        prefs = new AppPrefs(this);
        fx = FeedbackManager.get(this);
        setupMode = !pinStore.hasPin();

        title = findViewById(R.id.tvLockTitle);
        sub = findViewById(R.id.tvLockSub);
        dots = findViewById(R.id.pinDots);
        ((TextView) findViewById(R.id.tvBrand)).setLetterSpacing(0.3f);
        setPrompt(setupMode ? "Buat PIN baru" : "Masukkan PIN",
                setupMode ? "6 digit untuk melindungi tabunganmu" : "Buka brankas tabunganmu");

        boolean bio = !setupMode && prefs.biometricEnabled() && canBiometric();
        if (bio) initBiometric();
        buildKeypad(bio);
        if (bio) prompt.authenticate(promptInfo);
    }

    private void setPrompt(String t, String s) {
        title.setText(t);
        sub.setText(s);
    }

    private boolean canBiometric() {
        return BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void initBiometric() {
        prompt = new BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        dots.setFilled(6);
                        succeed();
                    }
                });
        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Buka SaveQuest")
                .setNegativeButtonText("Pakai PIN")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build();
    }

    // ---------- keypad ----------
    private void buildKeypad(boolean bio) {
        LinearLayout pad = findViewById(R.id.keypad);
        String[][] rows = {{"1", "2", "3"}, {"4", "5", "6"}, {"7", "8", "9"}, {"BIO", "0", "DEL"}};
        int size = Ui.dp(this, 74), margin = Ui.dp(this, 8);
        int order = 0;
        for (String[] r : rows) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER);
            for (String key : r) {
                View v;
                if (key.equals("BIO")) {
                    ImageView iv = icon(R.drawable.ic_fingerprint, 0xFF00E5FF);
                    if (bio) iv.setOnClickListener(x -> prompt.authenticate(promptInfo));
                    else iv.setVisibility(View.INVISIBLE);
                    v = iv;
                } else if (key.equals("DEL")) {
                    ImageView iv = icon(R.drawable.ic_backspace, 0xCCFFFFFF);
                    iv.setOnClickListener(x -> backspace());
                    iv.setOnLongClickListener(x -> {
                        clearEntry();
                        return true;
                    });
                    v = iv;
                } else {
                    TextView t = new TextView(this);
                    t.setText(key);
                    t.setTextColor(0xFFFFFFFF);
                    t.setTextSize(26);
                    t.setGravity(Gravity.CENTER);
                    t.setBackgroundResource(R.drawable.bg_key_glass);
                    t.setClickable(true);
                    t.setFocusable(true);
                    t.setOnClickListener(x -> digit(key));
                    v = t;
                }
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
                lp.setMargins(margin, margin, margin, margin);
                row.addView(v, lp);

                // animasi masuk bertahap
                v.setAlpha(0f);
                v.setTranslationY(Ui.dp(this, 24));
                v.animate().alpha(1f).translationY(0f).setStartDelay(order++ * 35L).setDuration(300).start();
            }
            pad.addView(row);
        }
    }

    private ImageView icon(int res, int tint) {
        ImageView iv = new ImageView(this);
        iv.setImageResource(res);
        iv.setColorFilter(tint);
        int p = Ui.dp(this, 22);
        iv.setPadding(p, p, p, p);
        iv.setBackgroundResource(R.drawable.bg_key_glass);
        iv.setClickable(true);
        iv.setFocusable(true);
        return iv;
    }

    // ---------- input ----------
    private void digit(String d) {
        if (busy || entry.length() >= PinStore.LENGTH) return;
        entry.append(d);
        fx.tick();
        dots.setFilled(entry.length());
        if (entry.length() == PinStore.LENGTH) submit();
    }

    private void backspace() {
        if (busy || entry.length() == 0) return;
        entry.deleteCharAt(entry.length() - 1);
        fx.tick();
        dots.setFilled(entry.length());
    }

    private void clearEntry() {
        if (busy) return;
        entry.setLength(0);
        dots.setFilled(0);
    }

    private void submit() {
        busy = true;
        final String pin = entry.toString();
        if (setupMode) {
            if (firstEntry == null) {
                firstEntry = pin;
                handler.postDelayed(() -> {
                    setPrompt("Konfirmasi PIN", "Masukkan PIN yang sama sekali lagi");
                    resetEntry();
                }, 220);
            } else if (firstEntry.equals(pin)) {
                pinStore.setPin(pin);
                succeed();
            } else {
                firstEntry = null;
                fail();
                setPrompt("PIN tidak sama", "Buat PIN baru dari awal");
            }
        } else if (pinStore.verify(pin)) {
            succeed();
        } else {
            fail();
            sub.setText("PIN salah, coba lagi");
        }
    }

    private void resetEntry() {
        entry.setLength(0);
        dots.setFilled(0);
        busy = false;
    }

    private void fail() {
        fx.error();
        dots.showError();
        handler.postDelayed(this::resetEntry, 450);
    }

    private void succeed() {
        busy = true;
        fx.success();
        dots.showSuccess();
        handler.postDelayed(this::unlock, 380);
    }

    private void unlock() {
        Session.unlocked = true;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
