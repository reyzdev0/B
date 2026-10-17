package com.savequest.app.ui.views;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

/** View avatar: latar + karakter + skin + bingkai. */
public class AvatarView extends View {
    private AvatarRenderer.Appearance appearance;

    public AvatarView(Context c) { super(c); }

    public AvatarView(Context c, AttributeSet a) { super(c, a); }

    public void setAppearance(AvatarRenderer.Appearance a) {
        appearance = a;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (appearance == null) return;
        float r = Math.min(getWidth(), getHeight()) / 2f;
        AvatarRenderer.draw(getContext(), canvas, getWidth() / 2f, getHeight() / 2f, r, appearance);
    }
}
