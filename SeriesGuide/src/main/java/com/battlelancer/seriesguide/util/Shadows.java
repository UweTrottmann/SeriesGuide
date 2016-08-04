package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;
import com.battlelancer.seriesguide.R;

/**
 * Helps create shadow drawables.
 */
public class Shadows {

    private static Shadows shadows;

    public static synchronized Shadows getInstance() {
        if (shadows == null) {
            shadows = new Shadows();
        }
        return shadows;
    }

    private int shadowColor = -1;

    public void setShadowDrawable(@NonNull Context context, @NonNull View shadowView,
            GradientDrawable.Orientation orientation) {
        if (shadowColor == -1) {
            shadowColor = ContextCompat.getColor(context,
                    Utils.resolveAttributeToResourceId(context.getTheme(),
                            R.attr.sgColorShadow));
        }
        GradientDrawable shadowDrawable = new GradientDrawable(orientation,
                new int[] { Color.TRANSPARENT, shadowColor });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            shadowView.setBackground(shadowDrawable);
        } else {
            //noinspection deprecation
            shadowView.setBackgroundDrawable(shadowDrawable);
        }
    }
}
