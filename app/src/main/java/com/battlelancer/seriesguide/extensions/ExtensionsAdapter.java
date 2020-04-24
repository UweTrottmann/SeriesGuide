package com.battlelancer.seriesguide.extensions;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.databinding.ItemExtensionBinding;
import com.battlelancer.seriesguide.util.ViewTools;

/**
 * Creates views for a list of {@link Extension}.
 */
public class ExtensionsAdapter extends ArrayAdapter<Extension> {

    public interface OnItemClickListener {
        void onExtensionMenuButtonClick(View anchor, Extension extension, int position);

        void onAddExtensionClick(View anchor);
    }

    private static final int VIEW_TYPE_EXTENSION = 0;
    private static final int VIEW_TYPE_ADD = 1;

    private final OnItemClickListener onItemClickListener;

    ExtensionsAdapter(Context context, OnItemClickListener onItemClickListener) {
        super(context, 0);
        this.onItemClickListener = onItemClickListener;
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
                        .inflate(R.layout.item_extension_add, parent, false);
            }
            TextView textViewAdd = convertView.findViewById(R.id.textViewItemExtensionAddLabel);
            ViewTools.setVectorIconLeft(getContext().getTheme(), textViewAdd,
                    R.drawable.ic_add_white_24dp);
            convertView
                    .setOnClickListener(v -> onItemClickListener.onAddExtensionClick(textViewAdd));
            return convertView;
        }

        ViewHolder viewHolder;
        if (convertView == null) {
            ItemExtensionBinding binding = ItemExtensionBinding
                    .inflate(LayoutInflater.from(parent.getContext()), parent, false);
            convertView = binding.getRoot();
            viewHolder = new ViewHolder(binding, onItemClickListener);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final Extension extension = getItem(position);
        if (extension != null) {
            viewHolder.bindTo(extension, position);
        }

        return convertView;
    }

    static class ViewHolder {
        private final ItemExtensionBinding binding;
        private final Drawable drawableIcon;
        @Nullable private Extension extension;
        int position;

        ViewHolder(ItemExtensionBinding binding, OnItemClickListener onItemClickListener) {
            this.binding = binding;
            binding.imageViewSettings.setOnClickListener(v ->
                    onItemClickListener.onExtensionMenuButtonClick(
                            binding.imageViewSettings, extension, position));
            drawableIcon = AppCompatResources.getDrawable(
                    binding.getRoot().getContext(), R.drawable.ic_extension_black_24dp);
        }

        void bindTo(Extension extension, int position) {
            this.extension = extension;
            this.position = position;

            binding.textViewTitle.setText(extension.label);
            binding.textViewDescription.setText(extension.description);
            if (extension.icon != null) {
                binding.imageViewIcon.setImageDrawable(extension.icon);
            } else {
                binding.imageViewIcon.setImageDrawable(drawableIcon);
            }
        }
    }
}
