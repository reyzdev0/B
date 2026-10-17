package com.savequest.app.ui.main;

import android.app.Activity;
import android.widget.Toast;

import com.savequest.app.R;
import com.savequest.app.data.local.entity.QuestEntity;
import com.savequest.app.data.repository.SaveResult;
import com.savequest.app.domain.BadgeCatalog;
import com.savequest.app.feedback.FeedbackManager;
import com.savequest.app.ui.common.DialogFactory;
import com.savequest.app.ui.common.SqDialog;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Menampilkan hasil operasi (haptic, toast, dialog) dengan cara yang sama di semua layar.
 * Dialog level up, boss, badge, dan quest diantre agar tampil satu per satu.
 */
public final class ResultPresenter {
    private final Activity act;
    private final MainViewModel vm;
    private final FeedbackManager fx;
    private final Deque<SqDialog.Builder> queue = new ArrayDeque<>();
    private boolean showing = false;

    public ResultPresenter(Activity act, MainViewModel vm) {
        this.act = act;
        this.vm = vm;
        this.fx = FeedbackManager.get(act);
    }

    public void present(SaveResult r) {
        if (r.error != null) {
            fx.error();
            toast(r.error);
            return;
        }

        switch (r.kind) {
            case SaveResult.DEPOSIT:
                fx.success();
                toast(r.summary());
                break;
            case SaveResult.WITHDRAW:
                if (r.expLost > 0 || r.hpDelta < 0) fx.error(); else fx.tick();
                toast(r.info);
                break;
            case SaveResult.CLAIM: {
                fx.claim();
                StringBuilder msg = new StringBuilder(r.info + "\n+" + r.expGained + " EXP, +" + r.coinsGained + " koin");
                for (String it : r.itemsGranted) msg.append("\nItem: ").append(it);
                enqueue(DialogFactory.reward(act, R.drawable.ic_target, SqDialog.GOLD,
                        "Hadiah diterima!", msg.toString(), "Mantap"));
                break;
            }
            case SaveResult.SPIN: {
                fx.claim();
                toast(r.info);
                break;
            }
            case SaveResult.HP:
                fx.error();
                toast(r.info);
                break;
            case SaveResult.ITEM:
                fx.success();
                toast(r.info);
                break;
            case SaveResult.SHOP:
                fx.success();
                if (r.info != null) toast(r.info);
                break;
            case SaveResult.BADGE:
                break;
            default:
                if (r.info != null) {
                    fx.tick();
                    toast(r.info);
                }
        }

        if (r.leveledUp) {
            fx.levelUp();
            enqueue(DialogFactory.levelUp(act, r.newLevel));
        }
        for (String boss : r.bossesDefeated) {
            String[] parts = boss.split("\u001F");
            fx.levelUp();
            StringBuilder msg = new StringBuilder(parts[0] + " tumbang!\n+" + parts[1] + " EXP, +" + parts[2] + " koin");
            for (String it : r.itemsGranted) msg.append("\nItem: ").append(it);
            enqueue(DialogFactory.reward(act, R.drawable.ic_skull, SqDialog.GOLD, "Boss kalah!",
                    msg.toString(), "Luar biasa"));
        }
        if (r.badges.size() > 3) {
            fx.levelUp();
            StringBuilder names = new StringBuilder();
            for (BadgeCatalog.Def d : r.badges) {
                if (names.length() > 0) names.append(", ");
                names.append(d.title);
            }
            enqueue(DialogFactory.reward(act, R.drawable.ic_trophy, SqDialog.PURPLE,
                    r.badges.size() + " badge terbuka!", names.toString(), "Keren"));
        } else {
            for (BadgeCatalog.Def d : r.badges) {
                fx.levelUp();
                enqueue(DialogFactory.badgeUnlock(act, d));
            }
        }
        if (!r.questsReady.isEmpty()) {
            final QuestEntity q = r.questsReady.get(0);
            enqueue(DialogFactory.questReady(act, q, () -> vm.claimQuest(q.id)));
        }
    }

    public void enqueue(SqDialog.Builder b) {
        queue.add(b);
        showNext();
    }

    private void showNext() {
        if (showing || act.isFinishing() || act.isDestroyed()) return;
        SqDialog.Builder next = queue.poll();
        if (next == null) return;
        showing = true;
        next.onDismiss(() -> {
            showing = false;
            showNext();
        }).show();
    }

    private void toast(String msg) {
        if (msg != null) Toast.makeText(act, msg, Toast.LENGTH_SHORT).show();
    }
}
