package com.chin.minddump.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Wraps AndroidX Biometric authentication for Private space access.
 * Uses BIOMETRIC_STRONG with DEVICE_CREDENTIAL as fallback (PIN/pattern/password).
 */
class BiometricHelper(private val activity: FragmentActivity) {

    fun authenticate(
        onSuccess: () -> Unit,
        onError: () -> Unit,
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("验证身份")
            .setSubtitle("进入 Private 空间需要身份验证")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    // User can retry — don't call onError yet
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError()
                }
            }
        )

        biometricPrompt.authenticate(promptInfo)
    }
}
