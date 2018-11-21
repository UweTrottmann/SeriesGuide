package com.battlelancer.seriesguide.ui.people;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TmdbTools;
import java.util.List;

/**
 * Shows a list of people in rows with headshots, name and description.
 */
class PeopleAdapter extends ArrayAdapter<PeopleListHelper.Person> {

    private static int LAYOUT = R.layout.item_person;

    PeopleAdapter(Context context) {
        super(context, LAYOUT);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(LAYOUT, parent, false);

            viewHolder = new ViewHolder();

            viewHolder.name = convertView.findViewById(R.id.textViewPerson);
            viewHolder.description = convertView.findViewById(
                    R.id.textViewPersonDescription);
            viewHolder.headshot = convertView.findViewById(R.id.imageViewPerson);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        PeopleListHelper.Person person = getItem(position);
        if (person == null) {
            return convertView;
        }

        // name and description
        viewHolder.name.setText(person.name);
        viewHolder.description.setText(person.description);

        // load headshot
        ServiceUtils.loadWithPicasso(getContext(),
                TmdbTools.buildProfileImageUrl(getContext(), person.profilePath,
                        TmdbTools.ProfileImageSize.W185))
                .resizeDimen(R.dimen.person_headshot_size, R.dimen.person_headshot_size)
                .centerCrop()
                .error(R.color.protection_dark)
                .into(viewHolder.headshot);

        return convertView;
    }

    /**
     * Replace the data in this {@link android.widget.ArrayAdapter} with the given list.
     */
    void setData(List<PeopleListHelper.Person> data) {
        clear();
        if (data != null) {
            addAll(data);
        }
    }

    static class ViewHolder {
        public TextView name;
        public TextView description;
        public ImageView headshot;
    }
}
