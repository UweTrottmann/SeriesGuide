package com.battlelancer.seriesguide.backend

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.databinding.FragmentCloudSetupBinding
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.sync.SyncProgress
import com.battlelancer.seriesguide.traktapi.ConnectTraktActivity
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.safeShow
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

    private var binding: FragmentCloudSetupBinding? = null

    private var snackbar: Snackbar? = null

    private lateinit var googleSignInClient: GoogleSignInClient
    private var signInAccount: GoogleSignInAccount? = null
    private lateinit var hexagonTools: HexagonTools

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hexagonTools = SgApp.getServicesComponent(requireContext()).hexagonTools()
        googleSignInClient = GoogleSignIn
            .getClient(requireActivity(), HexagonTools.googleSignInOptions)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCloudSetupBinding.inflate(inflater, container, false)

        binding!!.textViewCloudWarnings.setOnClickListener {
            // link to trakt account activity which has details about disabled features
            startActivity(Intent(context, ConnectTraktActivity::class.java))
        }

        binding!!.buttonCloudRemoveAccount.setOnClickListener {
            if (RemoveCloudAccountDialogFragment().safeShow(
                    parentFragmentManager,
                    "remove-cloud-account"
                )) {
                setProgressVisible(true)
            }
        }

        updateViews()
        setProgressVisible(true)
        binding!!.syncStatusCloud.visibility = View.GONE

        return binding!!.root
    }

    override fun onStart() {
        super.onStart()

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

    @Suppress("DEPRECATION") // Can't use ActivityResult API as third-party starts intent.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when {
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
        binding = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") event: RemoveCloudAccountDialogFragment.CanceledEvent) {
        setProgressVisible(false)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: RemoveCloudAccountDialogFragment.AccountRemovedEvent) {
        event.handle(requireContext())
        setProgressVisible(false)
        updateViews()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: SyncProgress.SyncEvent) {
        binding?.syncStatusCloud?.setProgress(event)
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

    private val signInWithGoogle =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            handleSignInResult(GoogleSignIn.getSignedInAccountFromIntent(result.data))
        }

    private fun signIn() {
        signInWithGoogle.launch(googleSignInClient.signInIntent)
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
        // hexagon enabled and account looks fine?
        if (HexagonSettings.isEnabled(context)
            && !HexagonSettings.shouldValidateAccount(context)) {
            binding?.textViewCloudUser?.text = HexagonSettings.getAccountName(activity)
            binding?.textViewCloudDescription?.setText(R.string.hexagon_description)

            // enable sign-out
            binding?.buttonCloudAction?.setText(R.string.hexagon_signout)
            binding?.buttonCloudAction?.setOnClickListener { signOut() }
            // enable account removal
            binding?.buttonCloudRemoveAccount?.visibility = View.VISIBLE
        } else {
            // did try to setup, but failed?
            if (!HexagonSettings.hasCompletedSetup(activity)) {
                // show error message
                binding?.textViewCloudDescription?.setText(R.string.hexagon_setup_incomplete)
            } else {
                binding?.textViewCloudDescription?.setText(R.string.hexagon_description)
            }
            binding?.textViewCloudUser?.text = null

            // enable sign-in
            binding?.buttonCloudAction?.setText(R.string.hexagon_signin)
            binding?.buttonCloudAction?.setOnClickListener {
                // restrict access to supporters
                if (Utils.hasAccessToX(activity)) {
                    startHexagonSetup()
                } else {
                    Utils.advertiseSubscription(activity)
                }
            }
            // disable account removal
            binding?.buttonCloudRemoveAccount?.visibility = View.GONE
        }
    }

    /**
     * Disables buttons and shows a progress bar.
     */
    private fun setProgressVisible(isVisible: Boolean) {
        binding?.progressBarCloudAccount?.visibility = if (isVisible) View.VISIBLE else View.GONE

        binding?.buttonCloudAction?.isEnabled = !isVisible
        binding?.buttonCloudRemoveAccount?.isEnabled = !isVisible
    }

    /**
     * Disables all buttons (use if signing in with Google seems not possible).
     */
    private fun setDisabled() {
        binding?.buttonCloudAction?.isEnabled = false
        binding?.buttonCloudRemoveAccount?.isEnabled = false
    }

    private fun showSnackbar(message: CharSequence) {
        dismissSnackbar()
        snackbar = Snackbar.make(requireView(), message, Snackbar.LENGTH_INDEFINITE).also {
            it.show()
        }
    }

    private fun dismissSnackbar() {
        snackbar?.dismiss()
    }

    private fun startHexagonSetup() {
        dismissSnackbar()
        setProgressVisible(true)

        val signInAccountOrNull = signInAccount
        if (signInAccountOrNull == null) {
            signIn()
        } else {
            Timber.i("Setting up Hexagon...")

            // set setup incomplete flag
            HexagonSettings.setSetupIncomplete(context)

            // validate account data
            val account = signInAccountOrNull.account
            if (TextUtils.isEmpty(signInAccountOrNull.email) || account == null) {
                Timber.d("Setting up Hexagon...FAILURE_AUTH")
                // show setup incomplete message + error toast
                view?.let {
                    Snackbar.make(it, R.string.hexagon_setup_fail_auth, Snackbar.LENGTH_LONG)
                        .show()
                }
            }
            // at last reset sync state, store the new credentials and enable hexagon integration
            else if (hexagonTools.setEnabled(signInAccountOrNull)) {
                // schedule full sync
                Timber.d("Setting up Hexagon...SUCCESS_SYNC_REQUIRED")
                SgSyncAdapter.requestSyncFullImmediate(activity, false)
                HexagonSettings.setSetupCompleted(activity)
            } else {
                // Do not set completed, will show setup incomplete message.
                Timber.d("Setting up Hexagon...FAILURE")
            }

            setProgressVisible(false)
            updateViews()
        }
    }

    companion object {
        private const val REQUEST_RESOLUTION = 2
        private const val ACTION_SIGN_IN = "sign-in"
    }
}
