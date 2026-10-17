package com.savequest.app.ui.avatar;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.savequest.app.R;
import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.domain.AvatarCatalog;
import com.savequest.app.domain.LevelSystem;
import com.savequest.app.feedback.FeedbackManager;
import com.savequest.app.ui.common.Ui;
import com.savequest.app.ui.main.MainViewModel;
import com.savequest.app.ui.main.SecureActivity;
import com.savequest.app.ui.views.AvatarRenderer;
import com.savequest.app.ui.views.AvatarView;
import com.savequest.app.util.Session;

/** Avatar Manager: pilih preset, lihat evolusi Hero, atau unggah foto dari galeri. */
public class AvatarActivity extends SecureActivity {
    private MainViewModel vm;
    private AvatarView preview;
    private TextView tvName, tvSub;
    private LinearLayout evoRow;
    private AvatarChoiceAdapter adapter;
    private PlayerEntity player;

    private final ActivityResultLauncher<String> picker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) importImage(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar);
        vm = new ViewModelProvider(this).get(MainViewModel.class);
        preview = findViewById(R.id.avatarPreview);
        tvName = findViewById(R.id.tvAvatarName);
        tvSub = findViewById(R.id.tvAvatarSub);
        evoRow = findViewById(R.id.evoRow);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnUpload).setOnClickListener(v -> pickImage());

        adapter = new AvatarChoiceAdapter(id -> {
            if (AvatarCatalog.CUSTOM.equals(id) && (player == null || player.customAvatarPath == null)) {
                pickImage();
            } else {
                vm.setAvatar(id);
            }
        });
        RecyclerView rv = findViewById(R.id.rvAvatars);
        rv.setLayoutManager(new GridLayoutManager(this, 3));
        rv.setAdapter(adapter);

        vm.player.observe(this, this::render);
        vm.result.observe(this, r -> {
            if (r == null) return;
            vm.result.setValue(null);
            FeedbackManager fx = FeedbackManager.get(this);
            if (r.error != null) fx.error(); else fx.tick();
            Toast.makeText(this, r.error != null ? r.error : r.info, Toast.LENGTH_SHORT).show();
        });
    }

    private void pickImage() {
        Session.markExternal();
        picker.launch("image/*");
    }

    private void importImage(Uri uri) {
        final Context app = getApplicationContext();
        new Thread(() -> {
            try {
                String path = AvatarImageUtil.saveSquare(app, uri);
                runOnUiThread(() -> vm.setCustomAvatar(path));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(app, "Foto gagal dimuat: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void render(PlayerEntity p) {
        if (p == null) return;
        player = p;
        int level = LevelSystem.levelFor(p.totalExp);
        preview.setAppearance(AvatarRenderer.Appearance.from(p));

        AvatarCatalog.Def def = AvatarCatalog.find(p.avatarId);
        tvName.setText(AvatarCatalog.CUSTOM.equals(p.avatarId) ? "Foto Saya" : def != null ? def.name : "Hero");
        tvSub.setText("Level " + level + "  |  " + AvatarCatalog.STAGE_NAMES[AvatarCatalog.stage(level)]);

        // baris evolusi Hero (5 tahap, terkunci sampai levelnya tercapai)
        evoRow.removeAllViews();
        AvatarCatalog.Def hero = AvatarCatalog.ALL.get(0);
        for (int i = 0; i < hero.drawables.length; i++) {
            AvatarRenderer.Appearance a = AvatarRenderer.Appearance.from(p);
            a.bitmap = null;
            a.avatarRes = hero.drawables[i];
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER_HORIZONTAL);
            AvatarView av = new AvatarView(this);
            av.setAppearance(a);
            boolean unlocked = level >= AvatarCatalog.STAGE_LEVELS[i];
            av.setAlpha(unlocked ? 1f : 0.3f);
            cell.addView(av, new LinearLayout.LayoutParams(Ui.dp(this, 52), Ui.dp(this, 52)));
            TextView t = new TextView(this);
            t.setText("Lv " + AvatarCatalog.STAGE_LEVELS[i]);
            t.setTextSize(11);
            t.setTextColor(Ui.color(this, unlocked ? R.color.sq_text : R.color.sq_text_dim));
            cell.addView(t);
            evoRow.addView(cell, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
        adapter.submit(p);
    }
}
