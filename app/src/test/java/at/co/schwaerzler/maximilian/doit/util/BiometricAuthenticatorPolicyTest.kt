package at.co.schwaerzler.maximilian.doit.util

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import org.junit.Assert.assertEquals
import org.junit.Test

class BiometricAuthenticatorPolicyTest {
    @Test
    fun allowsWeakBiometricsAndDeviceCredentialTogether() {
        assertEquals(BIOMETRIC_WEAK or DEVICE_CREDENTIAL, BiometricAuthenticatorPolicy.authenticators)
    }
}
