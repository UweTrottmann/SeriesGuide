
package com.battlelancer.seriesguide.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

/**
 * A utility class that helps with showing and hiding system UI such as the
 * status bar and navigation/system bar on API 11+ devices.
 * <p>
 * For more on system bars, see <a href=
 * "http://developer.android.com/design/get-started/ui-overview.html#system-bars"
 * > System Bars</a>.
 *
 * @see android.view.View#setSystemUiVisibility(int)
 * @see android.view.WindowManager.LayoutParams#FLAG_FULLSCREEN
 */
public class SystemUiHider {

    /**
     * When this flag is set, {@link #show()} and {@link #hide()} will toggle
     * the visibility of the status bar. If there is a navigation bar, show and
     * hide will toggle low profile mode.
     */
    public static final int FLAG_FULLSCREEN = 0x2;

    /**
     * When this flag is set, {@link #show()} and {@link #hide()} will toggle
     * the visibility of the navigation bar, if it's present on the device and
     * the device allows hiding it. In cases where the navigation bar is present
     * but cannot be hidden, show and hide will toggle low profile mode.
     */
    public static final int FLAG_HIDE_NAVIGATION = FLAG_FULLSCREEN | 0x4;

    /**
     * The activity associated with this UI hider object.
     */
    protected AppCompatActivity activity;

    /**
     * The view on which {@link View#setSystemUiVisibility(int)} will be called.
     */
    protected View anchorView;

    /**
     * The current UI hider flags.
     *
     * @see #FLAG_FULLSCREEN
     * @see #FLAG_HIDE_NAVIGATION
     */
    protected int flags;

    /**
     * Flags for {@link View#setSystemUiVisibility(int)} to use when showing the
     * system UI.
     */
    private int showFlags;

    /**
     * Flags for {@link View#setSystemUiVisibility(int)} to use when hiding the
     * system UI.
     */
    private int hideFlags;

    /**
     * Flags to test against the first parameter in
     * {@link android.view.View.OnSystemUiVisibilityChangeListener#onSystemUiVisibilityChange(int)}
     * to determine the system UI visibility state.
     */
    private int testFlags;

    /**
     * Whether or not the system UI is currently visible. This is cached from
     * {@link android.view.View.OnSystemUiVisibilityChangeListener}.
     */
    private boolean visible = true;

    /**
     * The current visibility callback.
     */
    protected OnVisibilityChangeListener onVisibilityChangeListener = sDummyListener;

    /**
     * Creates and returns an instance of {@link SystemUiHider}.
     *
     * @param activity The activity whose window's system UI should be controlled by this class.
     * @param anchorView The view on which {@link View#setSystemUiVisibility(int)} will be called.
     * @param flags Either 0 or any combination of {@link #FLAG_FULLSCREEN} and {@link
     * #FLAG_HIDE_NAVIGATION}.
     */
    public static SystemUiHider getInstance(AppCompatActivity activity, View anchorView,
            int flags) {
        return new SystemUiHider(activity, anchorView, flags);
    }

    /**
     * Constructor not intended to be called by clients. Use
     * {@link SystemUiHider#getInstance} to obtain an instance.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private SystemUiHider(AppCompatActivity activity, View anchorView, int flags) {
        this.activity = activity;
        this.anchorView = anchorView;
        this.flags = flags;

        showFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        hideFlags = View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        testFlags = View.SYSTEM_UI_FLAG_LOW_PROFILE;

        if ((this.flags & FLAG_FULLSCREEN) == FLAG_FULLSCREEN) {
            // If the client requested fullscreen, add flags relevant to hiding
            // the status bar. Note that some of these constants are new as of
            // API 16 (Jelly Bean). It is safe to use them, as they are inlined
            // at compile-time and do nothing on pre-Jelly Bean devices.
            showFlags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            hideFlags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        if ((this.flags & FLAG_HIDE_NAVIGATION) == FLAG_HIDE_NAVIGATION) {
            // If the client requested hiding navigation, add relevant flags.
            showFlags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
            hideFlags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            testFlags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }
    }

    /**
     * Sets up the system UI hider. Should be called from {@link AppCompatActivity#onCreate}.
     */
    public void setup() {
        anchorView.setOnSystemUiVisibilityChangeListener(systemUiVisibilityChangeListener);
    }

    /**
     * Returns whether or not the system UI is visible.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Hide the system UI.
     */
    public void hide() {
        anchorView.setSystemUiVisibility(hideFlags);
    }

    /**
     * Show the system UI.
     */
    public void show() {
        anchorView.setSystemUiVisibility(showFlags);
    }

    /**
     * Toggle the visibility of the system UI.
     */
    public void toggle() {
        if (isVisible()) {
            hide();
        } else {
            show();
        }
    }

    /**
     * Registers a callback, to be triggered when the system UI visibility
     * changes.
     */
    public void setOnVisibilityChangeListener(OnVisibilityChangeListener listener) {
        if (listener == null) {
            listener = sDummyListener;
        }

        onVisibilityChangeListener = listener;
    }

    /**
     * A dummy no-op callback for use when there is no other listener set.
     */
    private static OnVisibilityChangeListener sDummyListener = visible -> {
    };

    /**
     * A callback interface used to listen for system UI visibility changes.
     */
    public interface OnVisibilityChangeListener {
        /**
         * Called when the system UI visibility has changed.
         *
         * @param visible True if the system UI is visible.
         */
        void onVisibilityChange(boolean visible);
    }

    private View.OnSystemUiVisibilityChangeListener systemUiVisibilityChangeListener
            = new View.OnSystemUiVisibilityChangeListener() {
        @Override
        public void onSystemUiVisibilityChange(int vis) {
            // Test against testFlags to see if the system UI is visible.
            ActionBar supportActionBar = activity.getSupportActionBar();
            if ((vis & testFlags) != 0) {
                // As we use the appcompat toolbar as an action bar, we must manually hide it
                if (supportActionBar != null) {
                    supportActionBar.hide();
                }

                // Trigger the registered listener and cache the visibility state.
                onVisibilityChangeListener.onVisibilityChange(false);
                visible = false;
            } else {
                anchorView.setSystemUiVisibility(showFlags);

                // As we use the appcompat toolbar as an action bar, we must manually show it
                if (supportActionBar != null) {
                    supportActionBar.show();
                }

                // Trigger the registered listener and cache the visibility state.
                onVisibilityChangeListener.onVisibilityChange(true);
                visible = true;
            }
        }
    };
}
