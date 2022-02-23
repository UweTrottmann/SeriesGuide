package com.battlelancer.seriesguide.ui.dialogs

import android.app.Application
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import androidx.lifecycle.AndroidViewModel
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.sqlite.db.SimpleSQLiteQuery
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp.Companion.getServicesComponent
import com.battlelancer.seriesguide.databinding.DialogNotificationSelectionBinding
import com.battlelancer.seriesguide.provider.SeriesGuideContract.SgShow2Columns
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase.Tables
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.provider.SgShow2Notify
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences.UpdateSummariesEvent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.greenrobot.eventbus.EventBus

/**
 * A dialog displaying a list of shows with switches to turn notifications on or off.
 */
class NotificationSelectionDialogFragment : AppCompatDialogFragment() {

    private var binding: DialogNotificationSelectionBinding? = null
    private lateinit var adapter: SelectionAdapter
    private val model by viewModels<NotificationSelectionModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogNotificationSelectionBinding.inflate(layoutInflater)
        this.binding = binding

        adapter = SelectionAdapter(onItemClickListener)

        binding.apply {
            recyclerViewSelection.layoutManager = LinearLayoutManager(context)
            recyclerViewSelection.adapter = adapter
        }

        model.shows.observe(this) { shows ->
            val hasNoData = shows.isEmpty()
            this.binding?.textViewSelectionEmpty?.isGone = !hasNoData
            this.binding?.recyclerViewSelection?.isGone = hasNoData
            adapter.submitList(shows)
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        EventBus.getDefault().post(UpdateSummariesEvent())
    }

    private val onItemClickListener = object : SelectionAdapter.OnItemClickListener {
        override fun onItemClick(showId: Long, notify: Boolean) {
            getServicesComponent(requireContext()).showTools().storeNotify(showId, notify)
        }
    }

    class NotificationSelectionModel(application: Application) : AndroidViewModel(application) {

        val shows by lazy {
            val orderClause = if (DisplaySettings.isSortOrderIgnoringArticles(application)) {
                SgShow2Columns.SORT_TITLE_NOARTICLE
            } else {
                SgShow2Columns.SORT_TITLE
            }
            SgRoomDatabase.getInstance(application).sgShow2Helper()
                .getShowsNotifyStates(
                    SimpleSQLiteQuery(
                        "SELECT ${SgShow2Columns._ID}, ${SgShow2Columns.TITLE}, ${SgShow2Columns.NOTIFY} " +
                                "FROM ${Tables.SG_SHOW} " +
                                "ORDER BY $orderClause"
                    )
                )
        }

    }

    class SelectionAdapter(private val onItemClickListener: OnItemClickListener) :
        ListAdapter<SgShow2Notify, SelectionAdapter.ViewHolder>(SelectionDiffCallback()) {

        interface OnItemClickListener {
            fun onItemClick(showId: Long, notify: Boolean)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification_selection, parent, false)
            return ViewHolder(view, onItemClickListener)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val show = getItem(position)
            holder.showId = show.id
            holder.switchCompat.text = show.title
            holder.switchCompat.isChecked = show.notify
        }

        class ViewHolder(
            itemView: View,
            onItemClickListener: OnItemClickListener
        ) : RecyclerView.ViewHolder(itemView) {
            val switchCompat: SwitchCompat = itemView.findViewById(R.id.switchItemSelection)
            var showId = 0L

            init {
                itemView.setOnClickListener {
                    onItemClickListener.onItemClick(showId, switchCompat.isChecked)
                }
            }
        }

        class SelectionDiffCallback : DiffUtil.ItemCallback<SgShow2Notify>() {
            override fun areItemsTheSame(oldItem: SgShow2Notify, newItem: SgShow2Notify): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: SgShow2Notify,
                newItem: SgShow2Notify
            ): Boolean = oldItem == newItem
        }
    }
}