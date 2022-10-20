package com.battlelancer.seriesguide.shows.search.discover;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import com.battlelancer.seriesguide.databinding.LayoutAddIndicatorBinding;

public class AddIndicator extends FrameLayout {

    private final LayoutAddIndicatorBinding binding;

    public AddIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        binding = LayoutAddIndicatorBinding.inflate(LayoutInflater.from(context), this);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        binding.imageViewAddIndicatorAdded.setVisibility(GONE);
        binding.progressBarAddIndicator.setVisibility(GONE);
    }

    public void setContentDescriptionAdded(CharSequence contentDescription) {
        binding.imageViewAddIndicatorAdded.setContentDescription(contentDescription);
    }

    public void setOnAddClickListener(OnClickListener onClickListener) {
        binding.imageViewAddIndicator.setOnClickListener(onClickListener);
    }

    public void setState(int state) {
        if (state == SearchResult.STATE_ADD) {
            binding.imageViewAddIndicator.setVisibility(VISIBLE);
            binding.progressBarAddIndicator.setVisibility(GONE);
            binding.imageViewAddIndicatorAdded.setVisibility(GONE);
        } else if (state == SearchResult.STATE_ADDING) {
            binding.imageViewAddIndicator.setVisibility(GONE);
            binding.progressBarAddIndicator.setVisibility(VISIBLE);
            binding.imageViewAddIndicatorAdded.setVisibility(GONE);
        } else if (state == SearchResult.STATE_ADDED) {
            binding.imageViewAddIndicator.setVisibility(GONE);
            binding.progressBarAddIndicator.setVisibility(GONE);
            binding.imageViewAddIndicatorAdded.setVisibility(VISIBLE);
        }
    }
}
