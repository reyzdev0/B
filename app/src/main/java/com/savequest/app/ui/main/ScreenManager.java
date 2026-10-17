package com.savequest.app.ui.main;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.savequest.app.R;
import com.savequest.app.ui.badges.BadgesFragment;
import com.savequest.app.ui.home.HomeFragment;
import com.savequest.app.ui.settings.SettingsFragment;

/** Mengelola perpindahan layar: 0 = Badge, 1 = Home, 2 = Pengaturan. Animasi geser mengikuti arah. */
public class ScreenManager {
    private final FragmentManager fm;
    private final int containerId;
    private int current = -1;

    public ScreenManager(FragmentManager fm, int containerId) {
        this.fm = fm;
        this.containerId = containerId;
    }

    /** Dipakai saat Activity dipulihkan (fragment sudah dipulihkan oleh FragmentManager). */
    public void setCurrent(int index) { current = index; }

    public void show(int index, boolean animate) {
        if (index == current) return;
        FragmentTransaction tx = fm.beginTransaction().setReorderingAllowed(true);
        if (animate && current >= 0) {
            if (index > current) tx.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
            else tx.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
        }
        tx.replace(containerId, create(index), "screen" + index).commit();
        current = index;
    }

    private Fragment create(int index) {
        switch (index) {
            case 0: return new BadgesFragment();
            case 2: return new SettingsFragment();
            default: return new HomeFragment();
        }
    }
}
