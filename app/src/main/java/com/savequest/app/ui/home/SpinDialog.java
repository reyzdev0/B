package com.savequest.app.ui.home;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.savequest.app.R;
import com.savequest.app.domain.SpinSystem;
import com.savequest.app.ui.common.SqDialog;
import com.savequest.app.ui.common.Ui;
import com.savequest.app.ui.main.MainViewModel;
import com.savequest.app.ui.views.SpinWheelView;

/** Dialog roda keberuntungan harian. Hadiah ditentukan repository, roda hanya menganimasikan hasilnya. */
public final class SpinDialog {
    private SpinDialog() {}

    public static SqDialog.Builder create(Context c, MainViewModel vm) {
        final SpinWheelView wheel = new SpinWheelView(c);
        wheel.setLabels(SpinSystem.labels());
        final TextView status = new TextView(c);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setTextColor(Ui.color(c, R.color.sq_text));

        LinearLayout box = new LinearLayout(c);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.addView(wheel, new LinearLayout.LayoutParams(Ui.dp(c, 270), Ui.dp(c, 270)));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sp.topMargin = Ui.dp(c, 10);
        box.addView(status, sp);

        final int[] state = {0}; // 0 siap, 1 berputar, 2 selesai
        final SqDialog.Builder[] self = new SqDialog.Builder[1];
        self[0] = new SqDialog.Builder(c)
                .icon(R.drawable.ic_spin)
                .accent(SqDialog.GOLD)
                .title("Roda Keberuntungan")
                .message("Putar 1x sehari setelah menabung.")
                .body(box)
                .cancelable(false)
                .negative("Tutup", () -> state[0] != 1)
                .positive("Putar!", () -> {
                    if (state[0] == 2) return true;
                    if (state[0] == 1) return false;
                    state[0] = 1;
                    self[0].setPositiveText("Memutar...");
                    vm.spin(r -> {
                        if (r.error != null) {
                            state[0] = 0;
                            self[0].setPositiveText("Putar!");
                            vm.deliver(r);
                            return;
                        }
                        wheel.spinTo(r.spinIndex, idx -> {
                            state[0] = 2;
                            status.setText(r.info);
                            self[0].setPositiveText("Ambil hadiah");
                            vm.deliver(r);
                        });
                    });
                    return false;
                });
        return self[0];
    }
}
