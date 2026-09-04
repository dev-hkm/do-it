package at.co.schwaerzler.maximilian.doit.util

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL

internal object BiometricAuthenticatorPolicy {
    const val authenticators: Int = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
}
