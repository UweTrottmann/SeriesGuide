
package com.battlelancer.seriesguide.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;

/**
 * Tells about trakt and how it integrates with SeriesGuide, allows to proceed to entering
 * credentials step.
 */
public class ConnectTraktFragment extends Fragment {

    private Unbinder unbinder;
    @BindView(R.id.buttonPositive) Button buttonConnect;
    @BindView(R.id.buttonNegative) Button buttonCancel;
    @BindView(R.id.textViewAbout) TextView textViewAbout;
    @BindView(R.id.textViewTraktInfoHexagonWarning) TextView textViewHexagonWarning;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_connect_trakt_info, container, false);
        unbinder = ButterKnife.bind(this, v);

        buttonConnect.setText(R.string.connect_trakt);
        buttonCancel.setText(android.R.string.cancel);

        // wire up buttons
        buttonConnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                replaceWithCredentialsFragment();
            }
        });
        buttonCancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });

        // make learn more link clickable
        textViewAbout.setMovementMethod(LinkMovementMethod.getInstance());

        // show hexagon + trakt conflict warning
        textViewHexagonWarning.setVisibility(
                HexagonSettings.isEnabled(getActivity()) ? View.VISIBLE : View.GONE);

        return v;
    }

    private void replaceWithCredentialsFragment() {
        ConnectTraktCredentialsFragment f = ConnectTraktCredentialsFragment.newInstance();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, f);
        ft.commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }
}
