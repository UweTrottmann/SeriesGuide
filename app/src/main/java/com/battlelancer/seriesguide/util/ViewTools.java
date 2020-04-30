package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.battlelancer.seriesguide.R;

public class ViewTools {

    private ViewTools() {
    }

    // Note: VectorDrawableCompat has features/fixes backported to API 21-23.
    // https://medium.com/androiddevelopers/using-vector-assets-in-android-apps-4318fd662eb9
    public static void setVectorDrawableTop(TextView textView, @DrawableRes int vectorRes) {
        Drawable drawable = AppCompatResources.getDrawable(textView.getContext(), vectorRes);
        setCompoundDrawablesWithIntrinsicBounds(textView, null, drawable);
    }

    public static void setVectorDrawableLeft(TextView textView, @DrawableRes int vectorRes) {
        Drawable drawable = AppCompatResources.getDrawable(textView.getContext(), vectorRes);
        setCompoundDrawablesWithIntrinsicBounds(textView, drawable, null);
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end of, and below the
     * text. Use null if you do not want a Drawable there. The Drawables' bounds will be set to
     * their intrinsic bounds.
     */
    private static void setCompoundDrawablesWithIntrinsicBounds(TextView textView,
            Drawable left, Drawable top) {
        if (left != null) {
            left.setBounds(0, 0, left.getIntrinsicWidth(), left.getIntrinsicHeight());
        }
        if (top != null) {
            top.setBounds(0, 0, top.getIntrinsicWidth(), top.getIntrinsicHeight());
        }
        textView.setCompoundDrawables(left, top, null, null);
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
        swipeRefreshLayout.setColorSchemeResources(accentColorResId, R.color.sg_color_secondary);
    }

    public static void showSoftKeyboardOnSearchView(final Context context, final View searchView) {
        searchView.postDelayed(() -> {
            if (searchView.requestFocus()) {
                InputMethodManager imm = (InputMethodManager)
                        context.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
                }
            }
        }, 200); // have to add a little delay (http://stackoverflow.com/a/27540921/1000543)
    }

    public static void openUriOnClick(View button, final String uri) {
        if (button != null) {
            button.setOnClickListener(v -> Utils.launchWebsite(v.getContext(), uri));
        }
    }
}
