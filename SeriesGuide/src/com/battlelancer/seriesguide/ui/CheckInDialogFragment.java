
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.battlelancer.seriesguide.R;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CheckInDialogFragment extends SherlockDialogFragment {

    public static CheckInDialogFragment newInstance() {
        CheckInDialogFragment f = new CheckInDialogFragment();
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.checkin);
        final View layout = inflater.inflate(R.layout.checkin_dialog, null);

        return layout;
    }
}
