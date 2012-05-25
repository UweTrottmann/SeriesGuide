
package com.battlelancer.seriesguide;

import com.battlelancer.seriesguide.beta.R;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class WatchedBox extends ImageView {

    private boolean checked;

    public WatchedBox(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.checked = false;
        this.setImageResource(R.drawable.ic_action_watched);
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
            this.setImageResource(R.drawable.ic_watched);
        } else {
            this.setImageResource(R.drawable.ic_action_watched);
        }
    }
}
