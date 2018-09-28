package com.battlelancer.seriesguide.extensions;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.extensions.ExtensionManager.Extension;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.ViewTools;

/**
 * Creates views for a list of {@link Extension}.
 */
public class ExtensionsAdapter extends ArrayAdapter<Extension> {

    public interface OnItemClickListener {
        void onExtensionMenuButtonClick(View anchor, Extension extension, int position);

        void onAddExtensionClick(View anchor);
    }

    private static final int LAYOUT_EXTENSION = R.layout.item_extension;
    private static final int LAYOUT_ADD = R.layout.item_extension_add;

    private static final int VIEW_TYPE_EXTENSION = 0;
    private static final int VIEW_TYPE_ADD = 1;

    private final OnItemClickListener onItemClickListener;
    private final VectorDrawableCompat iconExtension;

    ExtensionsAdapter(Context context, OnItemClickListener onItemClickListener) {
        super(context, 0);
        this.onItemClickListener = onItemClickListener;
        iconExtension = ViewTools.vectorIconActive(context,
                context.getTheme(), R.drawable.ic_extension_black_24dp);
    }

    @Override
    public int getCount() {
        // extra row for add button
        return super.getCount() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        // last row is an add button
        return position == getCount() - 1 ? VIEW_TYPE_ADD : VIEW_TYPE_EXTENSION;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        if (getItemViewType(position) == VIEW_TYPE_ADD) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(LAYOUT_ADD, parent, false);
            }
            // warn non-supporters that they only can add a few extensions
            boolean isAtLimit =
                    getCount() - 1 == ExtensionsConfigurationFragment.EXTENSION_LIMIT_FREE
                            && !Utils.hasAccessToX(getContext());
            TextView textViewAdd = convertView.findViewById(R.id.textViewItemExtensionAddLabel);
            ViewTools.setVectorIconLeft(getContext().getTheme(), textViewAdd,
                    R.drawable.ic_add_white_24dp);
            textViewAdd.setVisibility(isAtLimit ? View.GONE : View.VISIBLE);
            convertView.findViewById(R.id.textViewItemExtensionAddLimit)
                    .setVisibility(isAtLimit ? View.VISIBLE : View.GONE);
            convertView
                    .setOnClickListener(v -> onItemClickListener.onAddExtensionClick(textViewAdd));
            return convertView;
        }

        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(LAYOUT_EXTENSION, parent, false);
            viewHolder = new ViewHolder(convertView, onItemClickListener);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final Extension extension = getItem(position);
        if (extension != null) {
            viewHolder.bindTo(extension, iconExtension, position);
        }

        return convertView;
    }

    static class ViewHolder {
        @BindView(R.id.imageViewItemExtensionIcon) ImageView icon;
        @BindView(R.id.textViewItemExtensionTitle) TextView title;
        @BindView(R.id.textViewItemExtensionDescription) TextView description;
        @BindView(R.id.imageViewItemExtensionSettings) ImageView settings;
        @Nullable private Extension extension;
        int position;

        public ViewHolder(View view, OnItemClickListener onItemClickListener) {
            ButterKnife.bind(this, view);
            settings.setOnClickListener(v -> onItemClickListener
                    .onExtensionMenuButtonClick(settings, extension, position));
        }

        void bindTo(Extension extension, VectorDrawableCompat iconExtension, int position) {
            this.extension = extension;
            this.position = position;

            title.setText(extension.label);
            description.setText(extension.description);
            if (extension.icon != null) {
                icon.setImageDrawable(extension.icon);
            } else {
                icon.setImageDrawable(iconExtension);
            }
        }
    }
}
