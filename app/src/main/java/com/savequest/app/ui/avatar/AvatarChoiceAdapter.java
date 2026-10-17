package com.savequest.app.ui.avatar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.savequest.app.R;
import com.savequest.app.data.local.entity.PlayerEntity;
import com.savequest.app.domain.AvatarCatalog;
import com.savequest.app.domain.LevelSystem;
import com.savequest.app.ui.views.AvatarRenderer;
import com.savequest.app.ui.views.AvatarView;

/** Grid pilihan avatar: preset (terkunci sesuai level) + slot foto galeri. */
public class AvatarChoiceAdapter extends RecyclerView.Adapter<AvatarChoiceAdapter.VH> {
    public interface OnChoose {
        void onChoose(String avatarId);
    }

    private PlayerEntity player;
    private final OnChoose listener;

    public AvatarChoiceAdapter(OnChoose listener) {
        this.listener = listener;
    }

    public void submit(PlayerEntity p) {
        player = p;
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        final AvatarView avatar;
        final View lock;
        final TextView name, status;

        VH(View v) {
            super(v);
            avatar = v.findViewById(R.id.choiceAvatar);
            lock = v.findViewById(R.id.choiceLock);
            name = v.findViewById(R.id.choiceName);
            status = v.findViewById(R.id.choiceStatus);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avatar_choice, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        if (player == null) return;
        int level = LevelSystem.levelFor(player.totalExp);
        AvatarRenderer.Appearance a = AvatarRenderer.Appearance.from(player);
        a.bitmap = null;

        String id;
        String name;
        boolean locked = false;
        String status;
        if (position < AvatarCatalog.ALL.size()) {
            AvatarCatalog.Def d = AvatarCatalog.ALL.get(position);
            id = d.id;
            name = d.name;
            a.avatarRes = AvatarCatalog.drawableFor(d.id, level);
            locked = level < d.minLevel;
            status = locked ? "Level " + d.minLevel : "";
        } else {
            id = AvatarCatalog.CUSTOM;
            name = "Foto Saya";
            boolean has = player.customAvatarPath != null;
            if (has) a.bitmap = AvatarRenderer.loadCustom(player.customAvatarPath);
            if (!has || a.bitmap == null) a.avatarRes = R.drawable.ic_avatar;
            status = has ? "" : "Unggah";
        }
        boolean selected = id.equals(player.avatarId);
        h.avatar.setAppearance(a);
        h.avatar.setAlpha(locked ? 0.4f : 1f);
        h.lock.setVisibility(locked ? View.VISIBLE : View.GONE);
        h.name.setText(name);
        h.status.setText(selected ? "Dipakai" : status);
        h.itemView.setOnClickListener(x -> listener.onChoose(id));
    }

    @Override
    public int getItemCount() {
        return AvatarCatalog.ALL.size() + 1;
    }
}
