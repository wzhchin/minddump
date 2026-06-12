package com.chin.minddump.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.fragment.app.FragmentActivity
import com.chin.minddump.ui.BiometricHelper

@Composable
fun BiometricGate(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onError: () -> Unit,
) {
    val biometricHelper = remember { BiometricHelper(activity) }
    LaunchedEffect(Unit) {
        biometricHelper.authenticate(
            onSuccess = onSuccess,
            onError = onError,
        )
    }
}
