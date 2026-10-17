package com.savequest.app.ui.common;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.savequest.app.R;

/**
 * Dialog kustom SaveQuest: kartu melengkung, latar redup + blur (Android 12+),
 * ikon gradient, tombol dengan ripple/gradient, dan animasi muncul.
 */
public final class SqDialog {
    private SqDialog() {}

    public static final int GREEN = 0, RED = 1, GOLD = 2, PURPLE = 3;

    /** Return true agar dialog ditutup. */
    public interface Click {
        boolean onClick();
    }

    public static final class Builder {
        private final Context ctx;
        private int icon = R.drawable.ic_vault;
        private int accent = GREEN;
        private CharSequence title = "", message, positive, negative;
        private View body;
        private Click onPositive, onNegative;
        private Runnable onDismiss;
        private boolean cancelable = true;
        private Dialog dialog;
        private TextView posView;

        public Builder(Context ctx) { this.ctx = ctx; }

        public Builder icon(int res) { icon = res; return this; }
        public Builder accent(int a) { accent = a; return this; }
        public Builder title(CharSequence t) { title = t; return this; }
        public Builder message(CharSequence m) { message = m; return this; }
        public Builder body(View v) { body = v; return this; }
        public Builder cancelable(boolean c) { cancelable = c; return this; }
        public Builder onDismiss(Runnable r) { onDismiss = r; return this; }

        public Builder positive(CharSequence text, Click c) {
            positive = text;
            onPositive = c;
            return this;
        }

        public Builder negative(CharSequence text, Click c) {
            negative = text;
            onNegative = c;
            return this;
        }

        public void setPositiveText(CharSequence t) {
            if (posView != null) posView.setText(t);
        }

        public void dismiss() {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
        }

        public Dialog show() {
            final Dialog d = new Dialog(ctx);
            dialog = d;
            d.requestWindowFeature(Window.FEATURE_NO_TITLE);
            View v = LayoutInflater.from(ctx).inflate(R.layout.dialog_sq, null);
            d.setContentView(v);
            d.setCancelable(cancelable);

            int[] col = Ui.accent(ctx, accent);
            FrameLayout iconWrap = v.findViewById(R.id.dlgIconWrap);
            iconWrap.setBackground(Ui.oval(col[0], col[1]));
            iconWrap.setElevation(Ui.dp(ctx, 6));
            ((ImageView) v.findViewById(R.id.dlgIcon)).setImageResource(icon);
            ((TextView) v.findViewById(R.id.dlgTitle)).setText(title);

            TextView msg = v.findViewById(R.id.dlgMsg);
            if (message != null && message.length() > 0) {
                msg.setText(message);
                msg.setVisibility(View.VISIBLE);
            }

            FrameLayout bodyWrap = v.findViewById(R.id.dlgBody);
            if (body != null) {
                if (body.getParent() instanceof ViewGroup) ((ViewGroup) body.getParent()).removeView(body);
                bodyWrap.addView(body);
            } else {
                bodyWrap.setVisibility(View.GONE);
            }

            TextView pos = v.findViewById(R.id.dlgPos);
            posView = pos;
            TextView neg = v.findViewById(R.id.dlgNeg);
            pos.setText(positive == null ? "OK" : positive);
            pos.setBackground(Ui.rippleGradient(col[0], col[1], Ui.dp(ctx, 14)));
            pos.setOnClickListener(x -> {
                if (onPositive == null || onPositive.onClick()) d.dismiss();
            });
            if (negative == null) {
                neg.setVisibility(View.GONE);
                ((ViewGroup.MarginLayoutParams) pos.getLayoutParams()).setMarginStart(0);
            } else {
                neg.setText(negative);
                neg.setOnClickListener(x -> {
                    if (onNegative == null || onNegative.onClick()) d.dismiss();
                });
            }

            d.setOnDismissListener(x -> {
                if (onDismiss != null) onDismiss.run();
            });

            Window w = d.getWindow();
            if (w != null) {
                w.setBackgroundDrawable(new InsetDrawable(new ColorDrawable(Color.TRANSPARENT), Ui.dp(ctx, 20)));
                w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
                w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                w.setDimAmount(0.6f);
                w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
                if (Build.VERSION.SDK_INT >= 31) {
                    w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                    WindowManager.LayoutParams lp = w.getAttributes();
                    lp.setBlurBehindRadius(24);
                    w.setAttributes(lp);
                }
            }

            View root = v.findViewById(R.id.dlgRoot);
            root.setScaleX(0.88f);
            root.setScaleY(0.88f);
            root.setAlpha(0f);
            d.show();
            root.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(240)
                    .setInterpolator(new OvershootInterpolator(1.1f)).start();
            return d;
        }
    }
}
