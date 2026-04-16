// SPDX-License-Identifier: Apache-2.0 AND AGPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2025 Google Inc. All Rights Reserved.
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

// Original file by Google Inc. licensed under Apache-2.0 copied from FirebaseUI-Android
// https://github.com/firebase/FirebaseUI-Android

package com.battlelancer.seriesguide.backend.auth.configuration.auth_provider

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.battlelancer.seriesguide.backend.auth.AuthException
import com.battlelancer.seriesguide.backend.auth.FirebaseAuthUI
import com.battlelancer.seriesguide.backend.auth.configuration.PasswordRule
import com.battlelancer.seriesguide.backend.auth.configuration.authUIConfiguration
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class EmailAuthProviderFirebaseAuthUIExtensionsTest {

    @Mock
    private lateinit var mockFirebaseAuth: FirebaseAuth

    private lateinit var firebaseApp: FirebaseApp
    private lateinit var applicationContext: Context

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        // Tests create their own FirebaseAuthUI instance, but to be sure clear any cached instance
        FirebaseAuthUI.clearInstanceCache()

        applicationContext = ApplicationProvider.getApplicationContext()

        FirebaseApp.getApps(applicationContext).forEach { app ->
            app.delete()
        }

        firebaseApp = FirebaseApp.initializeApp(
            applicationContext,
            FirebaseOptions.Builder()
                .setApiKey("fake-api-key")
                .setApplicationId("fake-app-id")
                .setProjectId("fake-project-id")
                .build()
        )
    }

    @After
    fun tearDown() {
        // Tests create their own FirebaseAuthUI instance, but to be sure clear any cached instance
        FirebaseAuthUI.clearInstanceCache()

        try {
            firebaseApp.delete()
        } catch (_: Exception) {
            // Ignore if already deleted
        }
    }

    @Test
    fun `createUserWithEmailAndPassword - create user with email and password succeeds`() =
        runTest {
            val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
            val emailProvider = AuthProvider.Email(
                emailLinkActionCodeSettings = null
            )
            val config = authUIConfiguration {
                context = applicationContext
                providers {
                    provider(emailProvider)
                }
            }

            val testPassword = "InsecureDoNotUse@123"
            // Just return null when calling Firebase API
            val taskCompletionSource = TaskCompletionSource<AuthResult>()
            taskCompletionSource.setResult(null)
            `when`(mockFirebaseAuth.createUserWithEmailAndPassword(TEST_EMAIL, testPassword))
                .thenReturn(taskCompletionSource.task)

            val result = instance.createUserWithEmailAndPassword(
                context = applicationContext,
                config = config,
                provider = emailProvider,
                name = null,
                email = TEST_EMAIL,
                password = testPassword
            )
            assertThat(result).isNull()

            // Check correct Firebase API would have been called
            verify(mockFirebaseAuth)
                .createUserWithEmailAndPassword(TEST_EMAIL, testPassword)
        }

    @Test
    fun `createUserWithEmailAndPassword - rejects password shorter than default min`() = runTest {
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        testCustomPasswordRule(
            instance = instance,
            rule = null, // default rules
            invalidPassword = "short",
            expectedReason = "Use at least 6 characters"
        )
    }

    @Test
    fun `createUserWithEmailAndPassword - rejects password not meeting custom rule`() = runTest {
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)

        testCustomPasswordRule(
            instance = instance,
            rule = PasswordRule.RequireDigit,
            invalidPassword = "InsecureDoNotUse",
            expectedReason = "Use at least one number"
        )
        testCustomPasswordRule(
            instance = instance,
            rule = PasswordRule.RequireLowercase,
            invalidPassword = "INSECUREDONOTUSE",
            expectedReason = "Use at least one lowercase letter"
        )
        testCustomPasswordRule(
            instance = instance,
            rule = PasswordRule.RequireUppercase,
            invalidPassword = "insecuredonotuse",
            expectedReason = "Use at least one uppercase letter"
        )
        testCustomPasswordRule(
            instance = instance,
            rule = PasswordRule.RequireSpecialCharacter,
            invalidPassword = "InsecureDoNotUse",
            expectedReason = "Use at least one special character"
        )
    }

    private fun testCustomPasswordRule(
        instance: FirebaseAuthUI,
        rule: PasswordRule?,
        invalidPassword: String,
        expectedReason: String
    ) {
        val emailProvider = if (rule != null) {
            AuthProvider.Email(
                emailLinkActionCodeSettings = null,
                additionalPasswordValidationRules = listOf(rule)
            )
        } else {
            // Use default settings
            AuthProvider.Email(
                emailLinkActionCodeSettings = null
            )
        }
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(emailProvider)
            }
        }

        // Just return null when calling Firebase API (only relevant in case the password check
        // incorrectly passes so the test doesn't fail with AuthException.UnknownException)
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(null)
        `when`(mockFirebaseAuth.createUserWithEmailAndPassword(anyString(), anyString()))
            .thenReturn(taskCompletionSource.task)

        val weakPasswordException = assertThrows(AuthException.WeakPasswordException::class.java) {
            runBlocking {
                instance.createUserWithEmailAndPassword(
                    context = applicationContext,
                    config = config,
                    provider = emailProvider,
                    name = null,
                    email = TEST_EMAIL,
                    password = invalidPassword
                )
            }
        }
        assertThat(weakPasswordException.reason).contains(expectedReason)

        // Firebase API to create user shouldn't get called
        verify(mockFirebaseAuth, never())
            .createUserWithEmailAndPassword(anyString(), anyString())
    }

    @Test
    fun `createUserWithEmailAndPassword - fails if new accounts not allowed`() = runTest {
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null,
            isNewAccountsAllowed = false
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(emailProvider)
            }
        }

        // Just return null when calling Firebase API
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(null)
        `when`(mockFirebaseAuth.createUserWithEmailAndPassword(anyString(), anyString()))
            .thenReturn(taskCompletionSource.task)

        val restrictedException = assertThrows(AuthException.AdminRestrictedException::class.java) {
            runBlocking {
                instance.createUserWithEmailAndPassword(
                    context = applicationContext,
                    config = config,
                    provider = emailProvider,
                    name = null,
                    email = TEST_EMAIL,
                    password = "InsecureDoNotUse@123"
                )
            }
        }
        assertThat(restrictedException.message).contains("Called despite provider.isNewAccountsAllowed = false")

        // Firebase API to create user shouldn't get called
        verify(mockFirebaseAuth, never())
            .createUserWithEmailAndPassword(anyString(), anyString())
    }

    @Test
    fun `createUserWithEmailAndPassword - handles account sign-up disabled server-side`() =
        runTest {
            val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
            val emailProvider = AuthProvider.Email(
                emailLinkActionCodeSettings = null
            )
            val config = authUIConfiguration {
                context = applicationContext
                providers {
                    provider(emailProvider)
                }
            }

            // Fake exception with admin restricted error code
            val firebaseRestrictedException = mock(FirebaseAuthException::class.java)
            `when`(firebaseRestrictedException.errorCode)
                .thenReturn(AuthException.FIREBASE_ERROR_ADMIN_RESTRICTED_OPERATION)

            val taskCompletionSource = TaskCompletionSource<AuthResult>()
            taskCompletionSource.setException(firebaseRestrictedException)
            `when`(mockFirebaseAuth.createUserWithEmailAndPassword(anyString(), anyString()))
                .thenReturn(taskCompletionSource.task)

            val restrictedException =
                assertThrows(AuthException.AdminRestrictedException::class.java) {
                    runBlocking {
                        instance.createUserWithEmailAndPassword(
                            context = applicationContext,
                            config = config,
                            provider = emailProvider,
                            name = null,
                            email = TEST_EMAIL,
                            password = "InsecureDoNotUse@123"
                        )
                    }
                }
            assertThat(restrictedException.message).contains("This action is restricted to admins")
        }

    @Test
    fun `signInWithEmailAndPassword - returns result on success`() = runTest {
        // Return fake auth result
        val mockAuthResult = mock(AuthResult::class.java)
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setResult(mockAuthResult)
        `when`(mockFirebaseAuth.signInWithEmailAndPassword(anyString(), anyString()))
            .thenReturn(taskCompletionSource.task)

        val result = signInWithTestEmailAndPassword()
        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(mockAuthResult)
    }

    @Test
    fun `signInWithEmailAndPassword - links given credential`() = runTest {
        val googleCredential = GoogleAuthProvider.getCredential("google-id-token", null)
        val mockUser = mock(FirebaseUser::class.java)

        // Return fake auth result
        val mockAuthResult = mock(AuthResult::class.java)
        `when`(mockAuthResult.user).thenReturn(mockUser)

        val signInTask = TaskCompletionSource<AuthResult>()
        signInTask.setResult(mockAuthResult)
        `when`(mockFirebaseAuth.signInWithEmailAndPassword(anyString(), anyString()))
            .thenReturn(signInTask.task)

        val linkTask = TaskCompletionSource<AuthResult>()
        linkTask.setResult(mockAuthResult)
        `when`(mockUser.linkWithCredential(googleCredential))
            .thenReturn(linkTask.task)

        val result = signInWithTestEmailAndPassword(credentialForLinking = googleCredential)
        assertThat(result).isNotNull()
        assertThat(result).isEqualTo(mockAuthResult)

        // Firebase API should have been called with given credential
        verify(mockUser).linkWithCredential(googleCredential)
    }

    @Test
    fun `signInWithEmailAndPassword - handles invalid credentials`() = runTest {
        // Fail API call with fake invalid credentials exception
        val invalidCredentialsException =
            FirebaseAuthInvalidCredentialsException("IGNORED", "ignored")
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setException(invalidCredentialsException)
        `when`(mockFirebaseAuth.signInWithEmailAndPassword(anyString(), anyString()))
            .thenReturn(taskCompletionSource.task)

        assertThrows(AuthException.InvalidCredentialsException::class.java) {
            runBlocking {
                signInWithTestEmailAndPassword()
            }
        }
    }

    @Test
    fun `signInWithEmailAndPassword - handles user not found`() = runTest {
        // Fail API call with fake invalid credentials exception
        val invalidUserException =
            FirebaseAuthInvalidUserException("IGNORED", "ignored")
        val taskCompletionSource = TaskCompletionSource<AuthResult>()
        taskCompletionSource.setException(invalidUserException)
        `when`(mockFirebaseAuth.signInWithEmailAndPassword(anyString(), anyString()))
            .thenReturn(taskCompletionSource.task)

        assertThrows(AuthException.UserNotFoundException::class.java) {
            runBlocking {
                signInWithTestEmailAndPassword()
            }
        }
    }

    private suspend fun signInWithTestEmailAndPassword(credentialForLinking: AuthCredential? = null): AuthResult? {
        val instance = FirebaseAuthUI.create(firebaseApp, mockFirebaseAuth)
        val emailProvider = AuthProvider.Email(
            emailLinkActionCodeSettings = null
        )
        val config = authUIConfiguration {
            context = applicationContext
            providers {
                provider(emailProvider)
            }
        }
        return instance.signInWithEmailAndPassword(
            context = applicationContext,
            config = config,
            provider = emailProvider,
            email = TEST_EMAIL,
            password = "InsecureDoNotUse@123",
            credentialForLinking = credentialForLinking,
            skipCredentialSave = true // Credential manager not set up for unit tests
        )
    }

    companion object {
        private const val TEST_EMAIL = "test@user.example"
    }

}