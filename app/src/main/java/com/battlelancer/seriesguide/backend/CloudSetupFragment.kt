// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.backend

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.databinding.FragmentCloudSetupBinding
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.sync.SyncProgress
import com.battlelancer.seriesguide.traktapi.ConnectTraktActivity
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.Utils
import com.battlelancer.seriesguide.util.safeShow
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import timber.log.Timber

/**
 * Manages signing in and out with Cloud and account removal.
 * Does not auto/silent sign-in when started so users need to explicitly sign in.
 * Enables Cloud on sign-in.
 * If Cloud is still enabled, but the account requires validation
 * enables to retry sign-in or to sign out (actually just disable Cloud).
 */
class CloudSetupFragment : Fragment() {

    private var binding: FragmentCloudSetupBinding? = null

    private var snackbar: Snackbar? = null

    private var signInAccount: FirebaseUser? = null
    private lateinit var hexagonTools: HexagonTools

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hexagonTools = SgApp.getServicesComponent(requireContext()).hexagonTools()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentCloudSetupBinding.inflate(inflater, container, false).also {
            binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding!!.apply {
            ThemeUtils.applyBottomPaddingForNavigationBar(scrollViewCloud)
            buttonCloudSignIn.setOnClickListener {
                // restrict access to supporters
                if (Utils.hasAccessToX(activity)) {
                    startHexagonSetup()
                } else {
                    Utils.advertiseSubscription(activity)
                }
            }
            buttonCloudSignOut.setOnClickListener { signOut() }

            textViewCloudWarnings.setOnClickListener {
                // link to trakt account activity which has details about disabled features
                startActivity(Intent(context, ConnectTraktActivity::class.java))
            }

            buttonCloudRemoveAccount.setOnClickListener {
                if (RemoveCloudAccountDialogFragment().safeShow(
                        parentFragmentManager,
                        "remove-cloud-account"
                    )) {
                    setProgressVisible(true)
                }
            }

            updateViews()
            setProgressVisible(true)
            syncStatusCloud.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        checkSignedIn()
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
     * If there is a signed in account, displays it.
     */
    private fun checkSignedIn() {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        // If not signed in account will be null.
        changeAccount(firebaseUser, null)
    }

    /**
     * If the Firebase account is not null, saves it and auto-starts setup if Cloud is not
     * enabled or the account needs validation.
     * On sign-in failure with error message (so was not canceled) sets should validate account flag.
     */
    private fun changeAccount(account: FirebaseUser?, errorIfNull: String?) {
        val signedIn = account != null
        if (signedIn) {
            Timber.i("Signed in to Cloud.")
            signInAccount = account
        } else {
            signInAccount = null
            errorIfNull?.let {
                HexagonSettings.shouldValidateAccount(requireContext(), true)
                showSnackbar(getString(R.string.hexagon_signin_fail_format, it))
            }
        }

        setProgressVisible(false)
        updateViews()

        if (signedIn && Utils.hasAccessToX(requireContext())) {
            if (!HexagonSettings.isEnabled(requireContext())
                || HexagonSettings.shouldValidateAccount(requireContext())) {
                Timber.i("Auto-start Cloud setup.")
                startHexagonSetup()
            }
        }
    }

    private val signInWithFirebase =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                changeAccount(FirebaseAuth.getInstance().currentUser, null)
            } else {
                val response = IdpResponse.fromResultIntent(result.data)
                if (response == null) {
                    // user chose not to sign in or add account, show no error message
                    changeAccount(null, null)
                } else {
                    val errorMessage: String?
                    val ex = response.error
                    if (ex != null) {
                        when (ex.errorCode) {
                            ErrorCodes.NO_NETWORK -> {
                                errorMessage = getString(R.string.offline)
                            }
                            ErrorCodes.PLAY_SERVICES_UPDATE_CANCELLED -> {
                                // user cancelled, show no error message
                                errorMessage = null
                            }
                            else -> {
                                if (ex.errorCode == ErrorCodes.DEVELOPER_ERROR
                                    && !hexagonTools.isGoogleSignInAvailable) {
                                    // Note: If trying to sign-in with email already used with
                                    // Google Sign-In on other device, fails to fall back to
                                    // Google Sign-In because Play Services is not available.
                                    errorMessage = getString(R.string.hexagon_signin_google_only)
                                } else {
                                    errorMessage = ex.message
                                    Errors.logAndReportHexagonAuthError(
                                        HexagonAuthError.build(ACTION_SIGN_IN, ex)
                                    )
                                }
                            }
                        }
                    } else {
                        errorMessage = "Unknown error"
                        Errors.logAndReportHexagonAuthError(
                            HexagonAuthError(ACTION_SIGN_IN, errorMessage)
                        )
                    }

                    changeAccount(null, errorMessage)
                }
            }
        }

    private fun signIn() {
        // Note: no need to provide a layout when just email sign-in is available
        // as Firebase UI will just directly proceed without asking for the provider.
        val authPickerLayout = AuthMethodPickerLayout.Builder(R.layout.auth_picker_email_google)
            .setEmailButtonId(R.id.buttonAuthSignInEmail)
            .setGoogleButtonId(R.id.buttonAuthSignInGoogle)
            .build()

        // Create and launch sign-in intent
        val intent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(hexagonTools.firebaseSignInProviders)
            .setIsSmartLockEnabled(hexagonTools.isGoogleSignInAvailable)
            .setTheme(SeriesGuidePreferences.THEME)
            .setAuthMethodPickerLayout(authPickerLayout)
            .build()

        signInWithFirebase.launch(intent)
    }

    private fun signOut() {
        if (HexagonSettings.shouldValidateAccount(requireContext())) {
            // Account needs to be repaired, so can't sign out, just disable Cloud
            hexagonTools.removeAccountAndSetDisabled()
            updateViews()
        } else {
            setProgressVisible(true)
            AuthUI.getInstance().signOut(requireContext()).addOnCompleteListener {
                Timber.i("Signed out.")
                signInAccount = null
                hexagonTools.removeAccountAndSetDisabled()
                if (this@CloudSetupFragment.isAdded) {
                    setProgressVisible(false)
                    updateViews()
                }
            }
        }
    }

    private fun updateViews() {
        if (HexagonSettings.isEnabled(requireContext())) {
            // hexagon enabled...
            binding?.textViewCloudUser?.text = HexagonSettings.getAccountName(requireContext())
            if (HexagonSettings.shouldValidateAccount(requireContext())) {
                // ...but account needs to be repaired
                binding?.textViewCloudDescription?.setText(R.string.hexagon_signed_out)
                setButtonsVisible(
                    signInVisible = true,
                    signOutVisible = true,
                    removeVisible = false
                )
            } else {
                // ...and account is fine
                binding?.textViewCloudDescription?.setText(R.string.hexagon_description)
                setButtonsVisible(
                    signInVisible = false,
                    signOutVisible = true,
                    removeVisible = true
                )
            }
        } else {
            // did try to setup, but failed?
            if (!HexagonSettings.hasCompletedSetup(requireContext())) {
                // show error message
                binding?.textViewCloudDescription?.setText(R.string.hexagon_setup_incomplete)
            } else {
                binding?.textViewCloudDescription?.setText(R.string.hexagon_description)
            }
            binding?.textViewCloudUser?.text = null
            setButtonsVisible(
                signInVisible = true,
                signOutVisible = false,
                removeVisible = false
            )
        }
    }

    private fun setButtonsVisible(
        signInVisible: Boolean,
        signOutVisible: Boolean,
        removeVisible: Boolean
    ) {
        binding?.apply {
            buttonCloudSignIn.isGone = !signInVisible
            buttonCloudSignOut.isGone = !signOutVisible
            buttonCloudRemoveAccount.isGone = !removeVisible
        }
    }

    /**
     * Disables buttons and shows a progress bar.
     */
    private fun setProgressVisible(isVisible: Boolean) {
        binding?.apply {
            progressBarCloudAccount.visibility = if (isVisible) View.VISIBLE else View.GONE

            buttonCloudSignIn.isEnabled = !isVisible
            buttonCloudSignOut.isEnabled = !isVisible
            buttonCloudRemoveAccount.isEnabled = !isVisible
        }
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
            HexagonSettings.setSetupIncomplete(requireContext())

            // validate account data
            if (signInAccountOrNull.email.isNullOrEmpty()) {
                Timber.d("Setting up Hexagon...FAILURE_AUTH")
                // show setup incomplete message + error toast
                view?.let {
                    Snackbar.make(it, R.string.hexagon_setup_fail_auth, Snackbar.LENGTH_LONG)
                        .show()
                }
            } else if (hexagonTools.setAccountAndEnabled(signInAccountOrNull)) {
                // schedule full sync
                Timber.d("Setting up Hexagon...SUCCESS_SYNC_REQUIRED")
                SgSyncAdapter.requestSyncFullImmediate(requireContext(), false)
                HexagonSettings.setSetupCompleted(requireContext())
            } else {
                // Do not set completed, will show setup incomplete message.
                Timber.d("Setting up Hexagon...FAILURE")
            }

            setProgressVisible(false)
            updateViews()
        }
    }

    companion object {
        private const val ACTION_SIGN_IN = "sign-in"
    }
}
