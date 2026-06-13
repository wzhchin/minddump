package com.chin.minddump.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.chin.minddump.R

/**
 * Wraps AndroidX Biometric authentication for Private space access.
 * Uses BIOMETRIC_STRONG with DEVICE_CREDENTIAL as fallback (PIN/pattern/password).
 */
class BiometricHelper(
    private val activity: FragmentActivity
) {
    fun authenticate(
        onSuccess: () -> Unit,
        onError: () -> Unit,
    ) {
        val promptInfo =
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(activity.getString(R.string.biometric_title))
                .setSubtitle(activity.getString(R.string.biometric_subtitle))
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                ).build()

        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt =
            BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess()
                    }

                    override fun onAuthenticationFailed() {
                        // User can retry — don't call onError yet
                    }

                    override fun onAuthenticationError(
                        errorCode: Int,
                        errString: CharSequence,
                    ) {
                        onError()
                    }
                },
            )

        biometricPrompt.authenticate(promptInfo)
    }
}
