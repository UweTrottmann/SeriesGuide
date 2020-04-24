package com.battlelancer.seriesguide.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.appcompat.content.res.AppCompatResources
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.ItemExtensionBinding

/**
 * Creates views for a list of [Extension].
 */
class ExtensionsAdapter(
    context: Context,
    private val onItemClickListener: OnItemClickListener
) : ArrayAdapter<Extension?>(context, 0) {

    interface OnItemClickListener {
        fun onExtensionMenuButtonClick(anchor: View, extension: Extension, position: Int)

        fun onAddExtensionClick(anchor: View)
    }

    override fun getCount(): Int {
        // extra row for add button
        return super.getCount() + 1
    }

    override fun getItemViewType(position: Int): Int {
        // last row is an add button
        return if (position == count - 1) VIEW_TYPE_ADD else VIEW_TYPE_EXTENSION
    }

    override fun getViewTypeCount(): Int = 2

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {
        if (getItemViewType(position) == VIEW_TYPE_ADD) {
            val view = convertView ?: LayoutInflater.from(parent.context).inflate(
                R.layout.item_extension_add, parent, false
            )!!
            val buttonAdd = view.findViewById<Button>(R.id.button_item_extension_add)
            buttonAdd.setOnClickListener {
                onItemClickListener.onAddExtensionClick(buttonAdd)
            }
            return view
        } else {
            val view = if (convertView != null) {
                convertView
            } else {
                val binding = ItemExtensionBinding
                    .inflate(LayoutInflater.from(parent.context), parent, false)
                binding.root.tag = ViewHolder(binding, onItemClickListener)
                binding.root
            }

            val extension = getItem(position)
            if (extension != null) {
                val viewHolder = view.tag as ViewHolder
                viewHolder.bindTo(extension, position)
            }

            return view
        }

    }

    internal class ViewHolder(
        private val binding: ItemExtensionBinding,
        onItemClickListener: OnItemClickListener
    ) {
        private val drawableIcon: Drawable?
        private var extension: Extension? = null
        var position = 0

        init {
            binding.imageViewSettings.setOnClickListener {
                extension?.let {
                    onItemClickListener.onExtensionMenuButtonClick(
                        binding.imageViewSettings, it, position
                    )
                }
            }
            drawableIcon = AppCompatResources.getDrawable(
                binding.root.context, R.drawable.ic_extension_black_24dp
            )
        }

        fun bindTo(extension: Extension, position: Int) {
            this.extension = extension
            this.position = position

            binding.textViewTitle.text = extension.label
            binding.textViewDescription.text = extension.description
            if (extension.icon != null) {
                binding.imageViewIcon.setImageDrawable(extension.icon)
            } else {
                binding.imageViewIcon.setImageDrawable(drawableIcon)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_EXTENSION = 0
        private const val VIEW_TYPE_ADD = 1
    }

}