package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import com.battlelancer.seriesguide.R;

/**
 * Helps create shadow drawables.
 */
public class Shadows {

    private static Shadows shadows;
    private int shadowColor;

    public static synchronized Shadows getInstance() {
        if (shadows == null) {
            shadows = new Shadows();
        }
        return shadows;
    }

    private Shadows() {
        shadowColor = -1;
    }

    public void setShadowDrawable(@NonNull Context context, @NonNull View shadowView,
            GradientDrawable.Orientation orientation) {
        if (shadowColor == -1) {
            shadowColor = ContextCompat.getColor(context,
                    Utils.resolveAttributeToResourceId(context.getTheme(),
                            R.attr.sgColorShadow));
        }
        GradientDrawable shadowDrawable = new GradientDrawable(orientation,
                new int[] { Color.TRANSPARENT, shadowColor });
        shadowView.setBackground(shadowDrawable);
    }

    public void resetShadowColor() {
        shadowColor = -1;
    }
}
