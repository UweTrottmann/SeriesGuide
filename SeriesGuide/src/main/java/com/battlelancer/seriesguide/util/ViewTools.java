package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;

public class ViewTools {

    private ViewTools() {
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end of, and below the
     * text.  Use 0 if you do not want a Drawable there. The Drawables' bounds will be set to their
     * intrinsic bounds.
     */
    public static void setCompoundDrawablesRelativeWithIntrinsicBounds(TextView textView,
            @DrawableRes int left, @DrawableRes int top, @DrawableRes int right,
            @DrawableRes int bottom) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(left, top, right, bottom);
            return;
        }

        Context context = textView.getContext();
        setCompoundDrawablesRelativeWithIntrinsicBounds(
                textView,
                left != 0 ? ContextCompat.getDrawable(context, left) : null,
                top != 0 ? ContextCompat.getDrawable(context, top) : null,
                right != 0 ? ContextCompat.getDrawable(context, right) : null,
                bottom != 0 ? ContextCompat.getDrawable(context, bottom) : null);
    }

    public static void setVectorAttrTop(Resources.Theme theme, TextView textView,
            @AttrRes int vectorAttr) {
        int vectorRes = Utils.resolveAttributeToResourceId(theme, vectorAttr);
        setVectorDrawableTop(theme, textView, vectorRes);
    }

    public static void setVectorDrawableTop(Resources.Theme theme, TextView textView,
            @DrawableRes int vectorRes) {
        setCompoundDrawableTop(textView,
                VectorDrawableCompat.create(textView.getResources(), vectorRes, theme));
    }

    public static void setVectorIcon(Resources.Theme theme, ImageView button,
            @DrawableRes int vectorRes) {
        button.setImageDrawable(vectorIconActive(button.getContext(), theme, vectorRes));
    }

    public static void setVectorIconLeft(Resources.Theme theme, TextView textView,
            @DrawableRes int vectorRes) {
        VectorDrawableCompat drawable = vectorIconActive(textView.getContext(), theme, vectorRes);
        if (drawable != null) {
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }
        textView.setCompoundDrawables(drawable, null, null, null);
    }

    public static void setVectorIconTop(Resources.Theme theme, TextView textView,
            @DrawableRes int vectorRes) {
        setCompoundDrawableTop(textView, vectorIconActive(textView.getContext(), theme, vectorRes));
    }

    public static VectorDrawableCompat vectorIconActive(Context context,
            Resources.Theme theme, @DrawableRes int vectorRes) {
        int colorRes = Utils.resolveAttributeToResourceId(theme, R.attr.sgColorIcon);
        return createTintedVectorDrawable(context, theme, vectorRes, colorRes);
    }

    public static VectorDrawableCompat vectorIconInactive(Context context,
            Resources.Theme theme, @DrawableRes int vectorRes) {
        int colorRes = Utils.resolveAttributeToResourceId(theme, R.attr.sgColorIconInactive);
        return createTintedVectorDrawable(context, theme, vectorRes, colorRes);
    }

    public static VectorDrawableCompat vectorIconWhite(Context context,
            Resources.Theme theme, @DrawableRes int vectorRes) {
        return createTintedVectorDrawable(context, theme, vectorRes, R.color.white);
    }

    private static VectorDrawableCompat createTintedVectorDrawable(Context context,
            Resources.Theme theme, @DrawableRes int vectorRes, @ColorRes int colorRes) {
        VectorDrawableCompat drawable = VectorDrawableCompat.create(context.getResources(),
                vectorRes, theme);
        if (drawable != null) {
            drawable.mutate();
            drawable.setTint(ContextCompat.getColor(context, colorRes));
        }
        return drawable;
    }

    private static void setCompoundDrawableTop(@NonNull TextView textView,
            @Nullable Drawable drawable) {
        if (drawable != null) {
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }
        textView.setCompoundDrawables(null, drawable, null, null);
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end of, and below the
     * text. Use null if you do not want a Drawable there. The Drawables' bounds will be set to
     * their intrinsic bounds.
     */
    public static void setCompoundDrawablesRelativeWithIntrinsicBounds(TextView textView,
            Drawable left, Drawable top, Drawable right, Drawable bottom) {
        if (left != null) {
            left.setBounds(0, 0, left.getIntrinsicWidth(), left.getIntrinsicHeight());
        }
        if (right != null) {
            right.setBounds(0, 0, right.getIntrinsicWidth(), right.getIntrinsicHeight());
        }
        if (top != null) {
            top.setBounds(0, 0, top.getIntrinsicWidth(), top.getIntrinsicHeight());
        }
        if (bottom != null) {
            bottom.setBounds(0, 0, bottom.getIntrinsicWidth(), bottom.getIntrinsicHeight());
        }
        textView.setCompoundDrawables(left, top, right, bottom);
    }

    public static void setValueOrPlaceholder(View view, final String value) {
        TextView field = (TextView) view;
        if (value == null || value.length() == 0) {
            field.setText(R.string.unknown);
        } else {
            field.setText(value);
        }
    }

    /**
     * If the given string is not null or empty, will make the label and value field {@link
     * View#VISIBLE}. Otherwise both {@link View#GONE}.
     *
     * @return True if the views are visible.
     */
    public static boolean setLabelValueOrHide(View label, TextView text, final String value) {
        if (TextUtils.isEmpty(value)) {
            label.setVisibility(View.GONE);
            text.setVisibility(View.GONE);
            return false;
        } else {
            label.setVisibility(View.VISIBLE);
            text.setVisibility(View.VISIBLE);
            text.setText(value);
            return true;
        }
    }

    /**
     * If the given double is larger than 0, will make the label and value field {@link
     * View#VISIBLE}. Otherwise both {@link View#GONE}.
     *
     * @return True if the views are visible.
     */
    public static boolean setLabelValueOrHide(View label, TextView text, double value) {
        if (value > 0.0) {
            label.setVisibility(View.VISIBLE);
            text.setVisibility(View.VISIBLE);
            text.setText(String.valueOf(value));
            return true;
        } else {
            label.setVisibility(View.GONE);
            text.setVisibility(View.GONE);
            return false;
        }
    }

    public static void setMenuItemActiveString(@NonNull MenuItem item) {
        item.setTitle(item.getTitle() + " ◀");
    }

    public static void setSwipeRefreshLayoutColors(Resources.Theme theme,
            SwipeRefreshLayout swipeRefreshLayout) {
        int accentColorResId = Utils.resolveAttributeToResourceId(theme, R.attr.colorAccent);
        swipeRefreshLayout.setColorSchemeResources(accentColorResId, R.color.teal_500);
    }

    public static void showSoftKeyboardOnSearchView(final Context context, final View searchView) {
        searchView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (searchView.requestFocus()) {
                    InputMethodManager imm = (InputMethodManager)
                            context.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 200); // have to add a little delay (http://stackoverflow.com/a/27540921/1000543)
    }
}
