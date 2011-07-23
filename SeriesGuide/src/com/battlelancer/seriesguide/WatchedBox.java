
package com.battlelancer.seriesguide;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.ImageView;
import com.battlelancer.seriesguide.beta.R;

public class WatchedBox extends ImageView {

    private boolean checked;

    private boolean hasFocus;

    public WatchedBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.checked = false;
        this.hasFocus = false;
        this.setImageResource(R.drawable.btn_check_on_disabled_holo_dark);
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
        updateStateImage();
    }

    public void toggle() {
        checked = !checked;
        updateStateImage();
    }

    private void updateStateImage() {
        if (checked) {
            if (hasFocus) {
                this.setImageResource(R.drawable.btn_check_on_focused_holo_dark);
            } else {
                this.setImageResource(R.drawable.btn_check_on_holo_dark);
            }
        } else {
            if (hasFocus) {
                this.setImageResource(R.drawable.btn_check_on_disabled_focused_holo_dark);
            } else {
                this.setImageResource(R.drawable.btn_check_on_disabled_holo_dark);
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (super.onKeyDown(keyCode, event)) {
            this.setImageResource(R.drawable.btn_check_on_pressed_holo_dark);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        this.hasFocus = gainFocus;
        updateStateImage();
    }
}
