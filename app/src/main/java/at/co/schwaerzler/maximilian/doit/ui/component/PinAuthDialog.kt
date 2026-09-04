/*
 * Copyright 2026 Maximilian Schwärzler
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package at.co.schwaerzler.maximilian.doit.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.co.schwaerzler.maximilian.doit.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class PinMode {
    AUTHENTICATE,
    SETUP_NEW,
    CONFIRM_NEW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinAuthDialog(
    correctPin: String?,
    mode: PinMode = PinMode.AUTHENTICATE,
    onSuccess: (String) -> Unit,
    onBiometricRequest: (() -> Unit)? = null,
    onDismissRequest: () -> Unit,
    title: String = stringResource(R.string.app_lock_title),
    subtitle: String? = null
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val incorrectPinStr = stringResource(R.string.incorrect_pin)
    val pinsDontMatchStr = stringResource(R.string.pins_dont_match)
    var currentPin by remember { mutableStateOf("") }
    var firstEnteredPin by remember { mutableStateOf("") }
    var currentMode by remember { mutableStateOf(mode) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val shakeOffset = remember { Animatable(0f) }

    val pinLength = 4

    fun triggerShake() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0
                    -20f at 50
                    20f at 100
                    -15f at 150
                    15f at 200
                    -8f at 250
                    8f at 300
                    0f at 350
                }
            )
        }
    }

    LaunchedEffect(currentPin) {
        if (currentPin.length == pinLength) {
            when (currentMode) {
                PinMode.AUTHENTICATE -> {
                    if (correctPin == null || currentPin == correctPin) {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onSuccess(currentPin)
                        onDismissRequest()
                    } else {
                        errorMessage = incorrectPinStr
                        triggerShake()
                        currentPin = ""
                    }
                }
                PinMode.SETUP_NEW -> {
                    firstEnteredPin = currentPin
                    currentPin = ""
                    currentMode = PinMode.CONFIRM_NEW
                    errorMessage = null
                }
                PinMode.CONFIRM_NEW -> {
                    if (currentPin == firstEnteredPin) {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onSuccess(currentPin)
                        onDismissRequest()
                    } else {
                        errorMessage = pinsDontMatchStr
                        triggerShake()
                        currentPin = ""
                        currentMode = PinMode.SETUP_NEW
                    }
                }
            }
        }
    }

    AppModalBottomSheet(
        onDismissRequest = onDismissRequest,
        title = when (currentMode) {
            PinMode.AUTHENTICATE -> title
            PinMode.SETUP_NEW -> stringResource(R.string.create_4digit_pin)
            PinMode.CONFIRM_NEW -> stringResource(R.string.confirm_4digit_pin)
        },
        subtitle = subtitle ?: when (currentMode) {
            PinMode.AUTHENTICATE -> stringResource(R.string.enter_pin_to_unlock)
            PinMode.SETUP_NEW -> stringResource(R.string.enter_pin_to_unlock)
            PinMode.CONFIRM_NEW -> stringResource(R.string.confirm_4digit_pin)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // PIN Dots Indicator with Shake Effect
            Row(
                modifier = Modifier
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pinLength) { index ->
                    val isFilled = index < currentPin.length
                    val dotColor = if (errorMessage != null) {
                        MaterialTheme.colorScheme.error
                    } else if (isFilled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    }

                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }

            // Error Message (if any)
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3x4 Numeric Keypad
            val keypad = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("bio", "0", "back")
            )

            keypad.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)
                ) {
                    row.forEach { key ->
                        when (key) {
                            "bio" -> {
                                if (onBiometricRequest != null && currentMode == PinMode.AUTHENTICATE) {
                                    Surface(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onBiometricRequest()
                                        },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        modifier = Modifier.size(68.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                painter = painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_scan_face),
                                                contentDescription = stringResource(R.string.biometric_unlock_setting),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.size(68.dp))
                                }
                            }
                            "back" -> {
                                Surface(
                                    onClick = {
                                        if (currentPin.isNotEmpty()) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            currentPin = currentPin.dropLast(1)
                                            errorMessage = null
                                        }
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.size(68.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_delete),
                                            contentDescription = "Backspace",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            else -> {
                                Surface(
                                    onClick = {
                                        if (currentPin.length < pinLength) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            currentPin += key
                                            errorMessage = null
                                        }
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.size(68.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
