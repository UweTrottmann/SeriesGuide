package com.battlelancer.seriesguide.backend

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.AsyncTask
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.sync.SyncProgress
import com.battlelancer.seriesguide.traktapi.ConnectTraktActivity
import com.battlelancer.seriesguide.traktapi.TraktCredentials
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.safeShow
import com.battlelancer.seriesguide.widgets.SyncStatusView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

/**
 * Helps connecting a device to Hexagon: sign in via Google account, initial uploading of shows.
 */
class CloudSetupFragment : Fragment() {

    @BindView(R.id.buttonCloudAction)
    internal lateinit var buttonAction: Button
    @BindView(R.id.textViewCloudDescription)
    internal lateinit var textViewDescription: TextView
    @BindView(R.id.textViewCloudUser)
    internal lateinit var textViewUsername: TextView
    @BindView(R.id.progressBarCloudAccount)
    internal lateinit var progressBarAccount: ProgressBar
    @BindView(R.id.syncStatusCloud)
    internal lateinit var syncStatusView: SyncStatusView
    @BindView(R.id.buttonCloudRemoveAccount)
    internal lateinit var buttonRemoveAccount: Button
    @BindView(R.id.textViewCloudWarnings)
    internal lateinit var textViewWarning: TextView
    private lateinit var unbinder: Unbinder

    private var snackbar: Snackbar? = null

    private lateinit var googleSignInClient: GoogleSignInClient
    private var signInAccount: GoogleSignInAccount? = null
    private lateinit var hexagonTools: HexagonTools
    private var hexagonSetupTask: HexagonSetupTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Try to keep the fragment around on config changes so the setup task
         * does not have to be finished.
         */
        retainInstance = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_cloud_setup, container, false)
        unbinder = ButterKnife.bind(this, v)

        textViewWarning.setOnClickListener {
            // link to trakt account activity which has details about disabled features
            startActivity(Intent(context, ConnectTraktActivity::class.java))
        }

        buttonRemoveAccount.setOnClickListener {
            if (RemoveCloudAccountDialogFragment().safeShow(
                    fragmentManager!!,
                    "remove-cloud-account"
                )) {
                setProgressVisible(true)
            }
        }

        updateViews()
        setProgressVisible(true)
        syncStatusView.visibility = View.GONE

        return v
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        hexagonTools = SgApp.getServicesComponent(context!!).hexagonTools()
        googleSignInClient = GoogleSignIn
            .getClient(activity!!, HexagonTools.getGoogleSignInOptions())
    }

    override fun onStart() {
        super.onStart()

        if (!isHexagonSetupRunning) {
            // check if the user is still signed in
            val signInTask = googleSignInClient.silentSignIn()
            if (signInTask.isSuccessful) {
                // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
                // and the GoogleSignInResult will be available instantly.
                Timber.d("Got cached sign-in")
                handleSignInResult(signInTask)
            } else {
                // If the user has not previously signed in on this device or the sign-in has expired,
                // this asynchronous branch will attempt to sign in the user silently.  Cross-device
                // single sign-on will occur in this branch.
                Timber.d("Trying async sign-in")
                signInTask.addOnCompleteListener { task ->
                    if (isAdded) {
                        handleSignInResult(task)
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
            requestCode == REQUEST_SIGN_IN -> {
                handleSignInResult(GoogleSignIn.getSignedInAccountFromIntent(data))
            }
            requestCode == REQUEST_RESOLUTION && resultCode == Activity.RESULT_OK -> {
                // not doing anything for now, user has to press sign-in button again
                Timber.i("Resolved an issue with Google sign-in.")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        unbinder.unbind()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isHexagonSetupRunning) {
            hexagonSetupTask?.cancel(true)
        }
        hexagonSetupTask = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") event: RemoveCloudAccountDialogFragment.CanceledEvent) {
        setProgressVisible(false)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: RemoveCloudAccountDialogFragment.AccountRemovedEvent) {
        event.handle(activity)
        setProgressVisible(false)
        updateViews()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: SyncProgress.SyncEvent) {
        syncStatusView.setProgress(event)
    }

    /**
     * On sign-in success, saves the signed in Google account and auto-starts setup if Cloud is not
     * enabled, yet. On sign-in failure disables Cloud.
     *
     * @param task A completed Google sign-in task.
     */
    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        var account: GoogleSignInAccount?
        var errorCodeString: String? = ""
        try {
            account = task.getResult(ApiException::class.java)
        } catch (e: ApiException) {
            account = null
            val statusCode = e.statusCode
            errorCodeString = GoogleSignInStatusCodes.getStatusCodeString(statusCode)
            when {
                statusCode == GoogleSignInStatusCodes.SIGN_IN_REQUIRED -> {
                    // never signed in or no account on device, show no error message
                    errorCodeString = null
                }
                statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED -> {
                    // user chose not to sign in or add account, show no error message
                    errorCodeString = null
                }
                statusCode == GoogleSignInStatusCodes.RESOLUTION_REQUIRED
                        && e is ResolvableApiException -> {
                    try {
                        e.startResolutionForResult(activity, REQUEST_RESOLUTION)
                    } catch (ignored: IntentSender.SendIntentException) {
                        // ignored
                    }
                }
                else -> Errors.logAndReport(
                    ACTION_SIGN_IN,
                    HexagonAuthError.build(ACTION_SIGN_IN, e)
                )
            }
        } catch (e: Exception) {
            account = null
            errorCodeString = e.message ?: ""
            Errors.logAndReport(ACTION_SIGN_IN, HexagonAuthError.build(ACTION_SIGN_IN, e))
        }

        val signedIn = account != null
        if (signedIn) {
            Timber.i("Signed in with Google.")
            signInAccount = account
        } else {
            signInAccount = null
            hexagonTools.setDisabled()
            errorCodeString?.let {
                showSnackbar(getString(R.string.hexagon_signin_fail_format, it))
            }
        }

        setProgressVisible(false)
        updateViews()

        if (signedIn && Utils.hasAccessToX(context)
            && !HexagonSettings.isEnabled(context)) {
            // auto-start setup if sign in succeeded and Cloud can be, but is not enabled, yet
            Timber.i("Auto-start Cloud setup.")
            startHexagonSetup()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, REQUEST_SIGN_IN)
    }

    private fun signOut() {
        setProgressVisible(true)
        googleSignInClient.signOut().addOnCompleteListener { task ->
            if (!this@CloudSetupFragment.isAdded) {
                return@addOnCompleteListener
            }

            val success = try {
                task.getResult(ApiException::class.java)
                true
            } catch (e: Exception) {
                Errors.logAndReport("sign-out", HexagonAuthError.build("sign-out", e))
                false
            }

            setProgressVisible(false)
            if (success) {
                Timber.i("Signed out of Google.")
                signInAccount = null
                hexagonTools.setDisabled()
                updateViews()
            }
        }
    }

    private fun updateViews() {
        // warn about changes in behavior with trakt
        textViewWarning.visibility = if (TraktCredentials.get(activity).hasCredentials()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        // hexagon enabled and account looks fine?
        if (HexagonSettings.isEnabled(context)
            && !HexagonSettings.shouldValidateAccount(context)) {
            textViewUsername.text = HexagonSettings.getAccountName(activity)
            textViewDescription.setText(R.string.hexagon_description)

            // enable sign-out
            buttonAction.setText(R.string.hexagon_signout)
            buttonAction.setOnClickListener { signOut() }
            // enable account removal
            buttonRemoveAccount.visibility = View.VISIBLE
        } else {
            // did try to setup, but failed?
            if (!HexagonSettings.hasCompletedSetup(activity)) {
                // show error message
                textViewDescription.setText(R.string.hexagon_setup_incomplete)
            } else {
                textViewDescription.setText(R.string.hexagon_description)
            }
            textViewUsername.text = null

            // enable sign-in
            buttonAction.setText(R.string.hexagon_signin)
            buttonAction.setOnClickListener {
                // restrict access to supporters
                if (Utils.hasAccessToX(activity)) {
                    startHexagonSetup()
                } else {
                    Utils.advertiseSubscription(activity)
                }
            }
            // disable account removal
            buttonRemoveAccount.visibility = View.GONE
        }
    }

    /**
     * Disables buttons and shows a progress bar.
     */
    private fun setProgressVisible(isVisible: Boolean) {
        progressBarAccount.visibility = if (isVisible) View.VISIBLE else View.GONE

        buttonAction.isEnabled = !isVisible
        buttonRemoveAccount.isEnabled = !isVisible
    }

    /**
     * Disables all buttons (use if signing in with Google seems not possible).
     */
    private fun setDisabled() {
        buttonAction.isEnabled = false
        buttonRemoveAccount.isEnabled = false
    }

    private fun showSnackbar(message: CharSequence) {
        dismissSnackbar()
        snackbar = Snackbar.make(view!!, message, Snackbar.LENGTH_INDEFINITE).also {
            it.show()
        }
    }

    private fun dismissSnackbar() {
        snackbar?.dismiss()
    }

    private fun startHexagonSetup() {
        dismissSnackbar()
        setProgressVisible(true)

        if (signInAccount == null) {
            signIn()
        } else if (!isHexagonSetupRunning) {
            HexagonSettings.setSetupIncomplete(context)
            hexagonSetupTask =
                HexagonSetupTask(
                    hexagonTools,
                    signInAccount!!,
                    onHexagonSetupFinishedListener
                ).also {
                    it.execute()
                }
        }
    }

    private val isHexagonSetupRunning: Boolean
        get() = hexagonSetupTask != null && hexagonSetupTask!!.status != AsyncTask.Status.FINISHED

    private class HexagonSetupTask
    /**
     * Checks for local and remote shows and uploads shows accordingly. If there are some shows
     * in the local database as well as on hexagon, will download and merge data first, then
     * upload.
     */
    internal constructor(
        private val hexagonTools: HexagonTools,
        private val signInAccount: GoogleSignInAccount,
        private val onSetupFinishedListener: OnSetupFinishedListener
    ) : AsyncTask<String, Void, Int>() {

        interface OnSetupFinishedListener {
            fun onSetupFinished(resultCode: Int)
        }

        override fun doInBackground(vararg params: String): Int {
            // set setup incomplete flag
            Timber.i("Setting up Hexagon...")

            // validate account data
            val account = signInAccount.account
            if (TextUtils.isEmpty(signInAccount.email) || account == null) {
                return FAILURE_AUTH
            }

            // at last reset sync state, store the new credentials and enable hexagon integration
            return if (!hexagonTools.setEnabled(signInAccount)) {
                FAILURE
            } else {
                SUCCESS_SYNC_REQUIRED
            }
        }

        override fun onPostExecute(result: Int) {
            onSetupFinishedListener.onSetupFinished(result)
        }

        companion object {
            internal const val SUCCESS_SYNC_REQUIRED = 1
            internal const val FAILURE = -1
            internal const val FAILURE_AUTH = -2
        }
    }

    private val onHexagonSetupFinishedListener = object : HexagonSetupTask.OnSetupFinishedListener {
        override fun onSetupFinished(resultCode: Int) {
            when (resultCode) {
                HexagonSetupTask.SUCCESS_SYNC_REQUIRED -> {
                    // schedule full sync
                    Timber.d("Setting up Hexagon...SUCCESS_SYNC_REQUIRED")
                    SgSyncAdapter.requestSyncFullImmediate(activity, false)
                    HexagonSettings.setSetupCompleted(activity)
                }
                HexagonSetupTask.FAILURE_AUTH -> {
                    // show setup incomplete message + error toast
                    view?.let {
                        Snackbar.make(it, R.string.hexagon_setup_fail_auth, Snackbar.LENGTH_LONG)
                            .show()
                    }
                    Timber.d("Setting up Hexagon...FAILURE_AUTH")
                }
                HexagonSetupTask.FAILURE -> {
                    // show setup incomplete message
                    Timber.d("Setting up Hexagon...FAILURE")
                }
            }

            if (view == null) {
                return
            }
            setProgressVisible(false) // allow new task
            updateViews()
        }
    }

    companion object {
        private const val REQUEST_SIGN_IN = 1
        private const val REQUEST_RESOLUTION = 2
        private const val ACTION_SIGN_IN = "sign-in"
    }
}
