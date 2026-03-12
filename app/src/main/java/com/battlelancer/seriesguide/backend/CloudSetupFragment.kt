// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2019 Uwe Trottmann <uwe@uwetrottmann.com>

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
import androidx.lifecycle.lifecycleScope
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.backend.auth.AuthException
import com.battlelancer.seriesguide.backend.auth.AuthFlowController
import com.battlelancer.seriesguide.backend.auth.FirebaseAuthActivity
import com.battlelancer.seriesguide.backend.auth.FirebaseAuthUI
import com.battlelancer.seriesguide.backend.auth.configuration.authUIConfiguration
import com.battlelancer.seriesguide.backend.auth.configuration.auth_provider.AuthProvider
import com.battlelancer.seriesguide.backend.settings.HexagonSettings
import com.battlelancer.seriesguide.billing.BillingTools
import com.battlelancer.seriesguide.databinding.FragmentCloudSetupBinding
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.sync.SyncProgress
import com.battlelancer.seriesguide.traktapi.ConnectTraktActivity
import com.battlelancer.seriesguide.util.Errors
import com.battlelancer.seriesguide.util.ThemeUtils
import com.battlelancer.seriesguide.util.safeShow
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.uwetrottmann.androidutils.AndroidUtils
import kotlinx.coroutines.launch
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

    private lateinit var hexagonTools: HexagonTools
    private var authController: AuthFlowController? = null

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
            buttonCloudSignIn.apply {
                if (!BillingTools.hasAccessToPaidFeatures()) {
                    (buttonCloudSignIn as MaterialButton).setIconResource(R.drawable.ic_awesome_black_24dp)
                }
                setOnClickListener {
                    signInOrStartSetupOrAdvertiseSubscription()
                }
            }

            buttonCloudSignOut.setOnClickListener {
                signOut()
            }

            textViewCloudWarnings.setOnClickListener {
                // link to trakt account activity which has details about disabled features
                startActivity(Intent(context, ConnectTraktActivity::class.java))
            }

            buttonCloudRemoveAccount.setOnClickListener {
                removeCloudAccount()
            }

            setProgressVisible(false)
            syncStatusCloud.visibility = View.GONE
        }
    }

    override fun onStart() {
        super.onStart()
        updateViewsWithCloudState()
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
        authController?.dispose()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onEvent(event: SyncProgress.SyncEvent) {
        binding?.syncStatusCloud?.setProgress(event)
    }

    private fun signInOrStartSetupOrAdvertiseSubscription() {
        if (BillingTools.hasAccessToPaidFeatures()) {
            if (!startSetupIfSignedIn()) {
                signIn()
            }
        } else {
            BillingTools.advertiseSubscription(requireContext())
        }
    }

    // FIXME
    private fun signIn() {
        dismissSnackbar() // clear any error from previous sign in attempt
        setProgressVisible(true)

        val authUI = FirebaseAuthUI.getInstance()
        val configuration = authUIConfiguration {
            context = requireContext().applicationContext
            providers {
                provider(
                    AuthProvider.Email(
                        emailLinkActionCodeSettings = null,
                        passwordValidationRules = emptyList()
                    )
                )
                if (hexagonTools.isGoogleSignInAvailable) {
                    provider(
                        AuthProvider.Google(
                            scopes = listOf("email"),
                            // TODO Created by google-services plugin, but should define manually
                            serverClientId = requireContext().getString(R.string.default_web_client_id),
                            filterByAuthorizedAccounts = false
                        )
                    )
                }
            }
        }

        val authController = authUI.createAuthFlow(configuration)
            .also { this.authController = it }

        val intent = authController.createIntent(requireContext())
        signInWithFirebase.launch(intent)

//        // Note: no need to provide a layout when just email sign-in is available
//        // as Firebase UI will just directly proceed without asking for the provider.
//        val authPickerLayout = AuthMethodPickerLayout.Builder(R.layout.auth_picker_email_google)
//            .setEmailButtonId(R.id.buttonAuthSignInEmail)
//            .setGoogleButtonId(R.id.buttonAuthSignInGoogle)
//            .build()
    }

    private val signInWithFirebase =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            authController?.dispose()
            setProgressVisible(false)
            if (result.resultCode == Activity.RESULT_OK) {
                startSetupIfSignedIn()
            } else {
                val error = result.data
                    ?.let {
                        if (AndroidUtils.isAtLeastTiramisu) {
                            it.getSerializableExtra(
                                FirebaseAuthActivity.EXTRA_ERROR,
                                AuthException::class.java
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            it.getSerializableExtra(FirebaseAuthActivity.EXTRA_ERROR)
                                    as? AuthException
                        }
                    }
                if (error == null) {
                    // user chose not to sign in or add account, show no error message
                    Timber.i("Sign in cancelled")
                } else {
                    val errorMessage: String?
                    when (error) {
                        is AuthException.NetworkException -> {
                            errorMessage = getString(R.string.offline)
                        }

                        is AuthException.AuthCancelledException -> {
                            // user cancelled, show no error message
                            errorMessage = null
                        }

                        else -> {
                            if (error is AuthException.AccountLinkingRequiredException
                                && !hexagonTools.isGoogleSignInAvailable) {
                                // Note: If trying to sign-in with email already used with
                                // Google Sign-In on other device, fails to fall back to
                                // Google Sign-In because Play Services is not available.
                                errorMessage = getString(R.string.hexagon_signin_google_only)
                            } else {
                                errorMessage = error.message
                                Timber.e(error, "Failed to sign in")
                                Errors.reportHexagonAuthError(ACTION_SIGN_IN, error)
                            }
                        }
                    }

                    errorMessage?.let {
                        HexagonSettings.setShouldValidateAccount(requireContext(), true)
                        updateViewsWithCloudState()
                        showSnackbar(getString(R.string.hexagon_signin_fail_format, it))
                    }
                }
            }
        }

    private fun signOut() {
        if (HexagonSettings.shouldValidateAccount(requireContext())) {
            // Account needs to be repaired, so can't sign out, just disable Cloud
            hexagonTools.removeAccountAndSetDisabled()
            updateViewsWithCloudState()
        } else {
            setProgressVisible(true)
            val context = requireContext().applicationContext
            viewLifecycleOwner.lifecycleScope.launch {
                // Launch on app scope to guarantee state in HexagonTools is updated even if this
                // fragment is destroyed.
                SgApp.coroutineScope.launch {
                    try {
                        FirebaseAuthUI.getInstance().signOut(context)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to sign out")
                        Errors.reportHexagonAuthError(ACTION_SIGN_OUT, e)
                    }
                    Timber.i("Signed out.")
                    hexagonTools.removeAccountAndSetDisabled()
                }.join()

                // If views aren't destroyed, yet, update them
                setProgressVisible(false)
                updateViewsWithCloudState()
            }
        }
    }

    private fun removeCloudAccount() {
        if (RemoveCloudAccountDialogFragment().safeShow(
                parentFragmentManager,
                "remove-cloud-account"
            )) {
            setProgressVisible(true)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(@Suppress("UNUSED_PARAMETER") event: RemoveCloudAccountDialogFragment.CanceledEvent) {
        setProgressVisible(false)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: RemoveCloudAccountDialogFragment.AccountRemovedEvent) {
        event.handle(requireContext())
        setProgressVisible(false)
        updateViewsWithCloudState()
    }

    private fun updateViewsWithCloudState() {
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

    private fun startSetupIfSignedIn(): Boolean {
        val account = FirebaseAuth.getInstance().currentUser
        val signedIn = account != null
        if (account != null) {
            Timber.i("Authenticated with Firebase")
            startHexagonSetup(account)
        }
        return signedIn
    }

    private fun startHexagonSetup(account: FirebaseUser) {
        dismissSnackbar()
        setProgressVisible(true)

        Timber.i("Setting up Hexagon...")
        // set setup incomplete flag
        HexagonSettings.setSetupIncomplete(requireContext())

        // validate account data
        if (account.email.isNullOrEmpty()) {
            Timber.d("Setting up Hexagon...FAILURE_AUTH")
            // show setup incomplete message + error toast
            view?.let {
                Snackbar.make(it, R.string.hexagon_setup_fail_auth, Snackbar.LENGTH_LONG)
                    .show()
            }
        } else if (hexagonTools.setAccountAndEnabled(account)) {
            // schedule full sync
            Timber.d("Setting up Hexagon...SUCCESS_SYNC_REQUIRED")
            SgSyncAdapter.requestSyncFullImmediate(requireContext(), false)
            HexagonSettings.setSetupCompleted(requireContext())
        } else {
            // Do not set completed, will show setup incomplete message.
            Timber.d("Setting up Hexagon...FAILURE")
        }

        setProgressVisible(false)
        updateViewsWithCloudState()
    }

    companion object {
        private const val ACTION_SIGN_IN = "sign-in"
        private const val ACTION_SIGN_OUT = "sign-out"
    }
}
