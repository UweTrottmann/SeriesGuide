package com.battlelancer.seriesguide.dataliberation

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.databinding.FragmentAutoBackupBinding
import com.battlelancer.seriesguide.dataliberation.DataLiberationFragment.LiberationResultEvent
import com.battlelancer.seriesguide.util.TaskManager
import com.battlelancer.seriesguide.util.tryLaunch
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * Configuration of auto backup, creation of optional copies
 * and ability to import the last auto backup.
 */
class AutoBackupFragment : Fragment() {

    private var binding: FragmentAutoBackupBinding? = null
    private val viewModel: AutoBackupViewModel by viewModels()
    private var isBackupAvailableForImport = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAutoBackupBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding ?: return

        // setup listeners
        binding.switchAutoBackup.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                BackupSettings.setAutoBackupEnabled(context)
                setContainerSettingsVisible(true)
            } else {
                BackupSettings.setAutoBackupDisabled(context)
                setContainerSettingsVisible(false)
            }
        }

        binding.buttonAutoBackupNow.setOnClickListener {
            if (TaskManager.getInstance().tryBackupTask(requireContext())) {
                setProgressLock(true)
            }
        }
        binding.buttonAutoBackupImport.setOnClickListener { runAutoBackupImport() }

        binding.checkBoxAutoBackupCreateCopy.isChecked =
            BackupSettings.isCreateCopyOfAutoBackup(context)
        binding.checkBoxAutoBackupCreateCopy
            .setOnCheckedChangeListener { buttonView: CompoundButton, isChecked: Boolean ->
                BackupSettings.setCreateCopyOfAutoBackup(buttonView.context, isChecked)
                updateFileViews()
            }

        binding.buttonAutoBackupShowsExportFile.setOnClickListener {
            createShowExportFileResult.tryLaunch(
                JsonExportTask.EXPORT_JSON_FILE_SHOWS,
                requireContext()
            )
        }
        binding.buttonAutoBackupListsExportFile.setOnClickListener {
            createListsExportFileResult.tryLaunch(
                JsonExportTask.EXPORT_JSON_FILE_LISTS,
                requireContext()
            )
        }
        binding.buttonAutoBackupMoviesExportFile.setOnClickListener {
            createMovieExportFileResult.tryLaunch(
                JsonExportTask.EXPORT_JSON_FILE_MOVIES,
                requireContext()
            )
        }

        binding.groupState.visibility = View.GONE
        updateFileViews()
        setProgressLock(false) // Also disables import button if backup availability unknown.

        // restore UI state
        if (viewModel.isImportTaskNotCompleted) {
            setProgressLock(true)
        }
        viewModel.availableBackupLiveData
            .observe(viewLifecycleOwner, { availableBackupTimeString: String? ->
                val lastBackupTimeString = availableBackupTimeString ?: "n/a"
                binding.textViewAutoBackupLastTime.text =
                    getString(R.string.last_auto_backup, lastBackupTimeString)

                isBackupAvailableForImport = availableBackupTimeString != null
                updateImportButtonState()

                // Also update status of last backup attempt.
                val errorOrNull = BackupSettings.getAutoBackupErrorOrNull(requireContext())
                when {
                    errorOrNull != null -> {
                        binding.groupState.visibility = View.VISIBLE

                        binding.imageViewBackupStatus
                            .setImageResource(R.drawable.ic_cancel_red_24dp)
                        binding.textViewBackupStatus.text =
                            getString(R.string.backup_failed) + " " + errorOrNull
                    }
                    isBackupAvailableForImport -> {
                        binding.groupState.visibility = View.VISIBLE

                        binding.imageViewBackupStatus
                            .setImageResource(R.drawable.ic_check_circle_green_24dp)
                        binding.textViewBackupStatus.setText(R.string.backup_success)
                    }
                    else -> {
                        // No error + no backup files.
                        binding.groupState.visibility = View.GONE
                    }
                }
            })
    }

    override fun onStart() {
        super.onStart()

        // update enabled state
        val autoBackupEnabled = BackupSettings.isAutoBackupEnabled(context)
        setContainerSettingsVisible(autoBackupEnabled)
        binding?.switchAutoBackup?.isChecked = autoBackupEnabled

        // Update auto-backup availability.
        viewModel.updateAvailableBackupData()

        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: LiberationResultEvent) {
        event.handle(view)
        if (!isAdded) {
            // don't touch views if fragment is not added to activity any longer
            return
        }
        viewModel.updateAvailableBackupData()
        updateFileViews()
        setProgressLock(false)
    }

    private fun runAutoBackupImport() {
        setProgressLock(true)

        val importTask = JsonImportTask(requireContext())
        viewModel.importTask = SgApp.coroutineScope.launch { importTask.run() }
    }

    private val createShowExportFileResult =
        registerForActivityResult(DataLiberationTools.CreateExportFileContract()) { uri ->
            storeBackupFile(JsonExportTask.BACKUP_SHOWS, uri)
        }

    private val createListsExportFileResult =
        registerForActivityResult(DataLiberationTools.CreateExportFileContract()) { uri ->
            storeBackupFile(JsonExportTask.BACKUP_LISTS, uri)
        }

    private val createMovieExportFileResult =
        registerForActivityResult(DataLiberationTools.CreateExportFileContract()) { uri ->
            storeBackupFile(JsonExportTask.BACKUP_MOVIES, uri)
        }

    private fun storeBackupFile(type: Int, uri: Uri?) {
        if (uri == null) return
        DataLiberationTools.tryToPersistUri(requireContext(), uri)
        BackupSettings.storeExportFileUri(requireContext(), type, uri, true)
        updateFileViews()
    }

    private fun setContainerSettingsVisible(visible: Boolean) {
        val binding = binding ?: return
        binding.containerAutoBackupSettings.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun updateImportButtonState() {
        val binding = binding ?: return
        binding.buttonAutoBackupImport.isEnabled = isBackupAvailableForImport
    }

    private fun setProgressLock(isLocked: Boolean) {
        val binding = binding ?: return
        binding.buttonAutoBackupNow.isEnabled = !isLocked
        binding.buttonAutoBackupImport.isEnabled = isBackupAvailableForImport && !isLocked
        binding.progressBarAutoBackup.visibility = if (isLocked) View.VISIBLE else View.GONE
        binding.buttonAutoBackupShowsExportFile.isEnabled = !isLocked
        binding.buttonAutoBackupListsExportFile.isEnabled = !isLocked
        binding.buttonAutoBackupMoviesExportFile.isEnabled = !isLocked
    }

    private fun updateFileViews() {
        val binding = binding ?: return
        if (BackupSettings.isCreateCopyOfAutoBackup(context)) {
            setUriOrPlaceholder(
                binding.textViewAutoBackupShowsExportFile,
                BackupSettings.getExportFileUri(
                    context, JsonExportTask.BACKUP_SHOWS, true
                )
            )
            setUriOrPlaceholder(
                binding.textViewAutoBackupListsExportFile,
                BackupSettings.getExportFileUri(
                    context, JsonExportTask.BACKUP_LISTS, true
                )
            )
            setUriOrPlaceholder(
                binding.textViewAutoBackupMoviesExportFile,
                BackupSettings.getExportFileUri(
                    context, JsonExportTask.BACKUP_MOVIES, true
                )
            )
            binding.groupUserFiles.visibility = View.VISIBLE
        } else {
            binding.groupUserFiles.visibility = View.GONE
        }
    }

    private fun setUriOrPlaceholder(textView: TextView, uri: Uri?) {
        textView.text = uri?.toString() ?: getString(R.string.no_file_selected)
        TextViewCompat.setTextAppearance(
            textView,
            if (uri == null) {
                R.style.TextAppearance_SeriesGuide_Body2_Error
            } else R.style.TextAppearance_SeriesGuide_Body2_Dim
        )
    }

}