// SPDX-License-Identifier: Apache-2.0
// Copyright 2013-2025 Uwe Trottmann

package com.battlelancer.seriesguide.dataliberation

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.databinding.FragmentDataLiberationBinding
import com.battlelancer.seriesguide.dataliberation.DataLiberationTools.CreateExportFileContract
import com.battlelancer.seriesguide.dataliberation.DataLiberationTools.SelectImportFileContract
import com.battlelancer.seriesguide.dataliberation.JsonExportTask.OnTaskProgressListener
import com.battlelancer.seriesguide.util.TextTools
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.WebTools
import com.battlelancer.seriesguide.util.tryLaunch
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * One button export or import of shows, lists and movies to or from a JSON file.
 * Uses Storage Access Framework so no permissions are required.
 */
class DataLiberationFragment : Fragment(), OnTaskProgressListener {

    private var binding: FragmentDataLiberationBinding? = null
    private val model: DataLiberationViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDataLiberationBinding.inflate(inflater, container, false)
        this.binding = binding
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding ?: return

        ThemeUtils.applyBottomPaddingForNavigationBar(binding.scrollViewDataLiberation)

        binding.progressBarDataLib.visibility = View.GONE

        // Import check boxes
        binding.checkBoxDataLibShows.setOnCheckedChangeListener { _, _ -> updateImportButtonEnabledState() }
        binding.checkBoxDataLibLists.setOnCheckedChangeListener { _, _ -> updateImportButtonEnabledState() }
        binding.checkBoxDataLibMovies.setOnCheckedChangeListener { _, _ -> updateImportButtonEnabledState() }

        binding.buttonDataLibImport.setOnClickListener {
            doDataImport()
        }

        // File select buttons
        binding.buttonDataLibShowsExport.setOnClickListener {
            createShowExportFileResult.tryLaunch(
                JsonExportTask.EXPORT_JSON_FILE_SHOWS,
                requireContext()
            )
        }
        binding.buttonDataLibShowsImportFile.setOnClickListener {
            selectShowsImportFileResult.tryLaunch(null, requireContext())
        }

        binding.buttonDataLibListsExport.setOnClickListener {
            createListsExportFileResult.tryLaunch(
                JsonExportTask.EXPORT_JSON_FILE_LISTS,
                requireContext()
            )
        }
        binding.buttonDataLibListsImportFile.setOnClickListener {
            selectListsImportFileResult.tryLaunch(null, requireContext())
        }

        binding.buttonDataLibMoviesExport.setOnClickListener {
            createMovieExportFileResult.tryLaunch(
                JsonExportTask.EXPORT_JSON_FILE_MOVIES,
                requireContext()
            )
        }
        binding.buttonDataLibMoviesImportFile.setOnClickListener {
            selectMoviesImportFileResult.tryLaunch(null, requireContext())
        }

        binding.buttonDataLibImportDocs.setOnClickListener {
            WebTools.openInApp(requireContext(), getString(R.string.url_import_documentation))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            model.importFiles.collect {
                binding.textViewDataLibShowsImportFile.text = it.fileNameShows ?: it.placeholderText
                binding.textViewDataLibListsImportFile.text = it.fileNameLists ?: it.placeholderText
                binding.textViewDataLibMoviesImportFile.text = it.fileNameMovies ?: it.placeholderText
            }
        }
        // Note: pre-check existing files for import, but do not overwrite any later changes
        if (savedInstanceState == null) {
            model.updateImportFileNames()
        }

        // restore UI state
        if (model.isDataLibTaskNotCompleted) {
            setProgressLock(true)
        }
    }

    private fun updateImportButtonEnabledState() {
        val binding = binding ?: return
        binding.buttonDataLibImport.isEnabled = (binding.checkBoxDataLibShows.isChecked
                || binding.checkBoxDataLibLists.isChecked
                || binding.checkBoxDataLibMovies.isChecked)
    }

    override fun onStart() {
        super.onStart()
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

    override fun onProgressUpdate(total: Int, completed: Int) {
        val binding = binding ?: return
        binding.progressBarDataLib.isIndeterminate = total == completed
        binding.progressBarDataLib.max = total
        binding.progressBarDataLib.progress = completed
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEvent(event: LiberationResultEvent) {
        event.handle(view)
        if (!isAdded) {
            // don't touch views if fragment is not added to activity any longer
            return
        }
        model.updateImportFileNames()
        setProgressLock(false)
    }

    private fun setProgressLock(isLocked: Boolean) {
        val binding = binding ?: return
        if (isLocked) {
            binding.buttonDataLibImport.isEnabled = false
        } else {
            updateImportButtonEnabledState()
        }
        binding.progressBarDataLib.visibility = if (isLocked) View.VISIBLE else View.GONE
        binding.checkBoxDataLibFullDump.isEnabled = !isLocked
        binding.buttonDataLibShowsExport.isEnabled = !isLocked
        binding.buttonDataLibShowsImportFile.isEnabled = !isLocked
        binding.buttonDataLibListsExport.isEnabled = !isLocked
        binding.buttonDataLibListsImportFile.isEnabled = !isLocked
        binding.buttonDataLibMoviesExport.isEnabled = !isLocked
        binding.buttonDataLibMoviesImportFile.isEnabled = !isLocked
        binding.checkBoxDataLibShows.isEnabled = !isLocked
        binding.checkBoxDataLibLists.isEnabled = !isLocked
        binding.checkBoxDataLibMovies.isEnabled = !isLocked
    }

    private fun doDataImport() {
        val binding = binding ?: return
        setProgressLock(true)

        val dataLibTask = JsonImportTask(
            requireContext(),
            binding.checkBoxDataLibShows.isChecked, binding.checkBoxDataLibLists.isChecked,
            binding.checkBoxDataLibMovies.isChecked
        )
        model.dataLibJob = SgApp.coroutineScope.launch { dataLibTask.run() }
    }

    private fun doDataExport(type: Int, uri: Uri?) {
        if (uri == null) return

        DataLiberationTools.tryToPersistUri(requireContext(), uri)
        BackupSettings.storeExportFileUri(context, type, uri, false)
        model.updateImportFileNames()

        val binding = binding ?: return
        setProgressLock(true)

        val exportTask = JsonExportTask(
            requireContext(),
            this@DataLiberationFragment,
            binding.checkBoxDataLibFullDump.isChecked, false, type
        )
        model.dataLibJob = exportTask.launch()
    }

    private val createShowExportFileResult =
        registerForActivityResult(CreateExportFileContract()) { uri ->
            doDataExport(JsonExportTask.BACKUP_SHOWS, uri)
        }

    private val createListsExportFileResult =
        registerForActivityResult(CreateExportFileContract()) { uri ->
            doDataExport(JsonExportTask.BACKUP_LISTS, uri)
        }

    private val createMovieExportFileResult =
        registerForActivityResult(CreateExportFileContract()) { uri ->
            doDataExport(JsonExportTask.BACKUP_MOVIES, uri)
        }

    private val selectShowsImportFileResult =
        registerForActivityResult(SelectImportFileContract()) { uri ->
            storeImportFileUri(JsonExportTask.BACKUP_SHOWS, uri)
            // For convenience and discoverability, enable for import after selecting file
            if (uri != null) {
                binding?.checkBoxDataLibShows?.isChecked = true
            }
        }

    private val selectListsImportFileResult =
        registerForActivityResult(SelectImportFileContract()) { uri ->
            storeImportFileUri(JsonExportTask.BACKUP_LISTS, uri)
            // For convenience and discoverability, enable for import after selecting file
            if (uri != null) {
                binding?.checkBoxDataLibLists?.isChecked = true
            }
        }

    private val selectMoviesImportFileResult =
        registerForActivityResult(SelectImportFileContract()) { uri ->
            storeImportFileUri(JsonExportTask.BACKUP_MOVIES, uri)
            // For convenience and discoverability, enable for import after selecting file
            if (uri != null) {
                binding?.checkBoxDataLibMovies?.isChecked = true
            }
        }

    private fun storeImportFileUri(type: Int, uri: Uri?) {
        if (uri == null) return
        DataLiberationTools.tryToPersistUri(requireContext(), uri)
        BackupSettings.storeImportFileUri(context, type, uri)
        model.updateImportFileNames()
    }

    class LiberationResultEvent {
        private val message: String?
        private val showIndefinite: Boolean

        constructor() {
            message = null
            showIndefinite = false
        }

        constructor(
            context: Context,
            message: String?,
            errorCause: String?,
            showIndefinite: Boolean
        ) {
            this.message = TextTools.dotSeparate(context, message, errorCause)
            this.showIndefinite = showIndefinite
        }

        fun handle(view: View?) {
            if (view != null && message != null) {
                val snackbar = Snackbar.make(
                    view, message,
                    if (showIndefinite) Snackbar.LENGTH_INDEFINITE else Snackbar.LENGTH_SHORT
                )
                val textView = snackbar.view.findViewById<TextView>(
                    com.google.android.material.R.id.snackbar_text
                )
                textView.maxLines = 5
                snackbar.show()
            }
        }
    }
}