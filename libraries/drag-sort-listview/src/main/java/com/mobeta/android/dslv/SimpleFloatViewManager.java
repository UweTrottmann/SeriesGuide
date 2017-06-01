package com.mobeta.android.dslv;

import android.content.Context;
import android.graphics.Point;
import android.os.Vibrator;
import android.view.View;
import android.widget.ListView;

/**
 * Simple implementation of the FloatViewManager class. Uses list items as they appear in the
 * ListView to create the floating View.
 */
public class SimpleFloatViewManager implements DragSortListView.FloatViewManager {

    private ListView listView;

    public SimpleFloatViewManager(ListView listView) {
        this.listView = listView;
    }

    /**
     * This simple implementation gets a new view of the list item currently shown at ListView
     * <code>position</code> from the adapter and triggers a short vibration.
     */
    @Override
    public View onCreateFloatView(int position) {
        // short vibrate to signal drag has started
        Vibrator v = (Vibrator) listView.getContext().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(10);

        // grab a new view for the item from the adapter
        return listView.getAdapter().getView(position, null, listView);
    }

    @Override
    public void onDragFloatView(View floatView, Point position, Point touch) {
        // do nothing
    }

    @Override
    public void onDestroyFloatView(View floatView) {
        // do nothing
    }
}

