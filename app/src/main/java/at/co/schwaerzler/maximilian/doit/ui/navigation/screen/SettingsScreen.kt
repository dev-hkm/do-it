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

package at.co.schwaerzler.maximilian.doit.ui.navigation.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import at.co.schwaerzler.maximilian.doit.R
import at.co.schwaerzler.maximilian.doit.data.SettingsViewModel
import at.co.schwaerzler.maximilian.doit.data.db.entity.Tag
import at.co.schwaerzler.maximilian.doit.ui.component.AppChoiceRow
import at.co.schwaerzler.maximilian.doit.ui.component.AppModalBottomSheet
import at.co.schwaerzler.maximilian.doit.ui.component.AppTopBar
import at.co.schwaerzler.maximilian.doit.ui.component.MaxWidthLayout
import at.co.schwaerzler.maximilian.doit.ui.component.PinAuthDialog
import at.co.schwaerzler.maximilian.doit.ui.component.PinMode
import at.co.schwaerzler.maximilian.doit.ui.component.TagListManagementSheet
import at.co.schwaerzler.maximilian.doit.util.AppFont
import at.co.schwaerzler.maximilian.doit.util.AppThemeMode
import at.co.schwaerzler.maximilian.doit.util.BiometricHelper
import at.co.schwaerzler.maximilian.doit.util.NotificationLeadTime
import at.co.schwaerzler.maximilian.doit.util.SortOrder
import at.co.schwaerzler.maximilian.doit.util.openUrl
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

@Composable
private fun SettingsSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp)
    )
}

/**
 * Settings screen showing app version, theme selector, tags, security & lock, and backup options.
 *
 * @param onNavigateBack Called when the user taps the back navigation icon.
 */
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val currentLocaleTag by viewModel.currentLocaleTag.collectAsStateWithLifecycle()
    val useDynamicColors by viewModel.useDynamicColors.collectAsStateWithLifecycle()
    val notificationLeadTime by viewModel.notificationLeadTime.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val appFont by viewModel.appFont.collectAsStateWithLifecycle()
    val tagsEnabled by viewModel.tagsEnabled.collectAsStateWithLifecycle()
    val cloudToken by viewModel.cloudBackupToken.collectAsStateWithLifecycle()
    val tags by viewModel.allTags.collectAsStateWithLifecycle()

    val appLockEnabled by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val appLockPin by viewModel.appLockPin.collectAsStateWithLifecycle()
    val biometricUnlockEnabled by viewModel.biometricUnlockEnabled.collectAsStateWithLifecycle()

    var cloudTokenInput by rememberSaveable { mutableStateOf(cloudToken) }
    var cloudBusy by remember { mutableStateOf(false) }
    var cloudStatus by remember { mutableStateOf<String?>(null) }
    var backupStatus by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(viewModel.exportTodosJson().toByteArray(Charsets.UTF_8))
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val json = context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            }
            if (json != null) {
                runCatching { viewModel.importTodosJson(json) }
                    .onSuccess { count -> backupStatus = context.getString(R.string.backup_import_success, count) }
                    .onFailure { backupStatus = context.getString(R.string.backup_import_error) }
            } else {
                backupStatus = context.getString(R.string.backup_import_error)
            }
        }
    }
    LaunchedEffect(cloudToken) { cloudTokenInput = cloudToken }

    SettingsScreenContent(
        onNavigateBack = onNavigateBack,
        themeMode = themeMode,
        onSetTheme = viewModel::setTheme,
        versionName = viewModel.versionName,
        currentLocaleTag = currentLocaleTag,
        supportedLocales = viewModel.supportedLocales,
        onSetLanguage = viewModel::setLanguage,
        notificationLeadTime = notificationLeadTime,
        onSetNotificationLeadTime = viewModel::setNotificationLeadTime,
        useDynamicColors = useDynamicColors,
        onSetUseDynamicColors = viewModel::setUseDynamicColor,
        sortOrder = sortOrder,
        onSetSortOrder = viewModel::setSortOrder,
        appFont = appFont,
        onSetAppFont = viewModel::setAppFont,
        tagsEnabled = tagsEnabled,
        onSetTagsEnabled = viewModel::setTagsEnabled,
        tags = tags,
        onAddTag = viewModel::createTag,
        onUpdateTag = viewModel::updateTag,
        onDeleteTag = viewModel::deleteTag,
        appLockEnabled = appLockEnabled,
        appLockPin = appLockPin,
        onSetAppLockEnabled = viewModel::setAppLockEnabled,
        onSetAppLockPin = viewModel::setAppLockPin,
        biometricUnlockEnabled = biometricUnlockEnabled,
        onSetBiometricUnlockEnabled = viewModel::setBiometricUnlockEnabled,
        onExportBackup = { exportLauncher.launch(context.getString(R.string.backup_export_filename)) },
        onImportBackup = { importLauncher.launch(arrayOf("application/json", "text/*")) },
        cloudToken = cloudTokenInput,
        onCloudTokenChange = { cloudTokenInput = it; viewModel.setCloudToken(it) },
        onGenerateCloudToken = { cloudTokenInput = viewModel.createCloudToken() },
        onUploadCloudBackup = {
            scope.launch {
                cloudBusy = true
                cloudStatus = viewModel.uploadCloudBackup(cloudTokenInput).fold(
                    onSuccess = { context.getString(R.string.cloud_backup_uploaded) },
                    onFailure = { context.getString(R.string.cloud_backup_failed, it.message ?: "Unknown error") }
                )
                cloudBusy = false
            }
        },
        onRestoreCloudBackup = {
            scope.launch {
                cloudBusy = true
                cloudStatus = viewModel.restoreCloudBackup(cloudTokenInput).fold(
                    onSuccess = { context.getString(R.string.cloud_backup_restored, it) },
                    onFailure = { context.getString(R.string.cloud_backup_failed, it.message ?: "Unknown error") }
                )
                cloudBusy = false
            }
        },
        cloudBusy = cloudBusy,
        cloudStatus = cloudStatus,
        backupStatus = backupStatus,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    onNavigateBack: () -> Unit,
    themeMode: AppThemeMode,
    onSetTheme: (AppThemeMode) -> Unit,
    versionName: String?,
    currentLocaleTag: String,
    supportedLocales: List<Pair<String, String>>,
    onSetLanguage: (String) -> Unit,
    notificationLeadTime: NotificationLeadTime,
    onSetNotificationLeadTime: (NotificationLeadTime) -> Unit,
    modifier: Modifier = Modifier,
    useDynamicColors: Boolean,
    onSetUseDynamicColors: (Boolean) -> Unit,
    sortOrder: SortOrder = SortOrder.DEADLINE_SOONEST_FIRST,
    onSetSortOrder: (SortOrder) -> Unit = {},
    tags: List<Tag> = emptyList(),
    onAddTag: (Tag) -> Unit = {},
    onUpdateTag: (Tag) -> Unit = {},
    onDeleteTag: (Tag) -> Unit = {},
    appLockEnabled: Boolean = false,
    appLockPin: String? = null,
    onSetAppLockEnabled: (Boolean) -> Unit = {},
    onSetAppLockPin: (String?) -> Unit = {},
    biometricUnlockEnabled: Boolean = false,
    onSetBiometricUnlockEnabled: (Boolean) -> Unit = {},
    onExportBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    appFont: AppFont = AppFont.NUNITO,
    onSetAppFont: (AppFont) -> Unit = {},
    tagsEnabled: Boolean = false,
    onSetTagsEnabled: (Boolean) -> Unit = {},
    cloudToken: String = "",
    onCloudTokenChange: (String) -> Unit = {},
    onGenerateCloudToken: () -> Unit = {},
    onUploadCloudBackup: () -> Unit = {},
    onRestoreCloudBackup: () -> Unit = {},
    cloudBusy: Boolean = false,
    cloudStatus: String? = null,
    backupStatus: String? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val githubUrl = stringResource(R.string.github_repo_url)
    val githubReleasesBaseUrl = stringResource(R.string.github_releases_base_url)
    val fdroidUrl = stringResource(R.string.fdroid_package_url)

    var showLanguageSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showFontSheet by remember { mutableStateOf(false) }
    var showLeadTimeSheet by remember { mutableStateOf(false) }
    var showSortOrderSheet by remember { mutableStateOf(false) }
    var showTagManagerSheet by remember { mutableStateOf(false) }

    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPinAuthToDisableLock by remember { mutableStateOf(false) }

    val currentLanguageDisplay = if (currentLocaleTag.isEmpty()) {
        stringResource(R.string.settings_language_system_default)
    } else {
        val locale = Locale.forLanguageTag(currentLocaleTag)
        locale.getDisplayName(locale).replaceFirstChar { it.uppercaseChar() }
    }
    val listItemColors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.background
    )
    val itemShape = RoundedCornerShape(16.dp)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            AppTopBar(
                title = {
                    Text(
                        stringResource(R.string.settings_app_bar),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateBack()
                    }) {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_arrow_left), contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        MaxWidthLayout(Modifier.padding(top = innerPadding.calculateTopPadding())) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp)
            ) {
                // Section: General
                SettingsSectionHeader(stringResource(R.string.settings_section_general))

                ListItem(
                    headlineContent = { Text(stringResource(R.string.theme), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(stringResource(themeMode.labelRes)) },
                    leadingContent = {
                        Icon(painterResource(themeMode.iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(itemShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showThemeSheet = true
                        },
                    colors = listItemColors
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.dynamic_color_setting_label), fontWeight = FontWeight.Medium) },
                    leadingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_palette), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Switch(
                            checked = useDynamicColors,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onSetUseDynamicColors(it)
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(itemShape),
                    colors = listItemColors
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_font), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(stringResource(appFont.labelRes)) },
                    leadingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_type), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(itemShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showFontSheet = true
                        },
                    colors = listItemColors
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_language), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(currentLanguageDisplay) },
                    leadingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_languages), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(itemShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showLanguageSheet = true
                        },
                    colors = listItemColors
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.tags_enabled_setting), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(stringResource(R.string.tags_enabled_subtitle)) },
                    leadingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_tag), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Switch(
                            checked = tagsEnabled,
                            onCheckedChange = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onSetTagsEnabled(it)
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(itemShape),
                    colors = listItemColors
                )

                // Tags Management Item
                ListItem(
                    headlineContent = { Text(stringResource(R.string.tags_management_title), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(stringResource(R.string.tags_management_count_template, tags.size)) },
                    leadingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_tag), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(itemShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showTagManagerSheet = true
                        },
                    colors = listItemColors
                )

                // Section: Security & App Lock
                SettingsSectionHeader(stringResource(R.string.security_lock_section))

                ListItem(
                    headlineContent = { Text(stringResource(R.string.app_lock_title), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(stringResource(R.string.app_lock_description)) },
                    leadingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_shield_check), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { shouldEnable ->
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                if (shouldEnable) {
                                    if (appLockPin.isNullOrBlank()) {
                                        showPinSetupDialog = true
                                    } else {
                                        onSetAppLockEnabled(true)
                                    }
                                } else {
                                    if (appLockPin != null) {
                                        showPinAuthToDisableLock = true
                                    } else {
                                        onSetAppLockEnabled(false)
                                    }
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(itemShape),
                    colors = listItemColors
                )

                if (appLockPin != null) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.change_pin_title), fontWeight = FontWeight.Medium) },
                        supportingContent = { Text(stringResource(R.string.change_pin_subtitle)) },
                        leadingContent = {
                            Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_key_round), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .clip(itemShape)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showPinSetupDialog = true
                            },
                        colors = listItemColors
                    )
                }

                if (BiometricHelper.isBiometricAvailable(context)) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.biometric_unlock_setting), fontWeight = FontWeight.Medium) },
                        supportingContent = { Text(stringResource(R.string.biometric_unlock_subtitle)) },
                        leadingContent = {
                            Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_scan_face), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingContent = {
                            Switch(
                                checked = biometricUnlockEnabled,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                         if (!it) {
                                             onSetBiometricUnlockEnabled(false)
                                         } else if (context is FragmentActivity) {
                                             BiometricHelper.promptBiometric(
                                                 activity = context,
                                                 title = context.getString(R.string.biometric_unlock_setting),
                                                 subtitle = context.getString(R.string.biometric_unlock_subtitle),
                                                 onSuccess = { onSetBiometricUnlockEnabled(true) },
                                                 onError = { _, _ -> onSetBiometricUnlockEnabled(false) },
                                             )
                                         }
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .clip(itemShape),
                        colors = listItemColors
                    )
                }

                // Section: Notifications & Sorting
                SettingsSectionHeader(stringResource(R.string.settings_section_behavior))

                ListItem(
                    headlineContent = { Text(stringResource(R.string.notification_lead_time_title), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(stringResource(notificationLeadTime.labelRes)) },
                    leadingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_bell), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(itemShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showLeadTimeSheet = true
                        },
                    colors = listItemColors
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_sort_order), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(stringResource(sortOrder.labelRes)) },
                    leadingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_arrow_down_wide_narrow), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(itemShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showSortOrderSheet = true
                        },
                    colors = listItemColors
                )

                // Section: Backup & Restore
                SettingsSectionHeader(stringResource(R.string.backup_section_title))

                ListItem(
                    headlineContent = { Text(stringResource(R.string.backup_export_title), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(stringResource(R.string.backup_export_subtitle)) },
                    leadingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_upload), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(itemShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onExportBackup()
                        },
                    colors = listItemColors
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.backup_import_title), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(stringResource(R.string.backup_import_subtitle)) },
                    leadingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_download), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(itemShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onImportBackup()
                        },
                    colors = listItemColors
                )

                if (backupStatus != null) {
                    Text(
                        text = backupStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }

                // Cloud Backup Token Section
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_cloud_backup),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.size(10.dp))
                            Column {
                                Text(stringResource(R.string.backup_section_title), fontWeight = FontWeight.SemiBold)
                                Text(
                                    stringResource(R.string.cloud_backup_help),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = cloudToken,
                            onValueChange = onCloudTokenChange,
                            label = { Text(stringResource(R.string.cloud_backup_token_label)) },
                            placeholder = { Text(stringResource(R.string.cloud_backup_token_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onGenerateCloudToken()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(stringResource(R.string.cloud_backup_new_token))
                            }
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                    onUploadCloudBackup()
                                },
                                modifier = Modifier.weight(1.25f),
                                shape = RoundedCornerShape(14.dp),
                                enabled = !cloudBusy && cloudToken.isNotBlank()
                            ) {
                                Text(stringResource(R.string.cloud_backup_upload))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                onRestoreCloudBackup()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !cloudBusy && cloudToken.isNotBlank()
                        ) {
                            Text(stringResource(R.string.cloud_backup_restore))
                        }
                        if (cloudBusy) {
                            Spacer(Modifier.height(12.dp))
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                        if (cloudStatus != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = cloudStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Section: About
                SettingsSectionHeader(stringResource(R.string.settings_section_about))

                ListItem(
                    headlineContent = { Text(stringResource(R.string.version), fontWeight = FontWeight.Medium) },
                    supportingContent = { Text(versionName ?: stringResource(R.string.version_unknown)) },
                    leadingContent = {
                        Icon(painterResource(com.composables.icons.lucide.R.drawable.lucide_ic_info), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp)
                        .clip(itemShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            context.openUrl("$githubReleasesBaseUrl/tag/$versionName")
                        },
                    colors = listItemColors
                )
            }
        }
    }

    if (showTagManagerSheet) {
        TagListManagementSheet(
            tags = tags,
            onAddTag = onAddTag,
            onUpdateTag = onUpdateTag,
            onDeleteTag = onDeleteTag,
            onDismissRequest = { showTagManagerSheet = false }
        )
    }

    if (showPinSetupDialog) {
        PinAuthDialog(
            correctPin = null,
            mode = PinMode.SETUP_NEW,
            onSuccess = { newPin ->
                onSetAppLockPin(newPin)
                onSetAppLockEnabled(true)
                showPinSetupDialog = false
            },
            onDismissRequest = { showPinSetupDialog = false }
        )
    }

    if (showPinAuthToDisableLock) {
        PinAuthDialog(
            correctPin = appLockPin,
            title = stringResource(R.string.enter_pin_to_disable_lock),
            onSuccess = {
                onSetAppLockEnabled(false)
                showPinAuthToDisableLock = false
            },
            onDismissRequest = { showPinAuthToDisableLock = false }
        )
    }

    if (showThemeSheet) {
        AppModalBottomSheet(
            onDismissRequest = { showThemeSheet = false },
            title = stringResource(R.string.theme),
            subtitle = stringResource(R.string.settings_theme_dialog_title)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppThemeMode.entries.forEach { mode ->
                    AppChoiceRow(
                        selected = themeMode == mode,
                        onClick = {
                            onSetTheme(mode)
                            showThemeSheet = false
                        },
                        label = stringResource(mode.labelRes),
                        leadingIcon = painterResource(mode.iconRes)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showFontSheet) {
        AppModalBottomSheet(
            onDismissRequest = { showFontSheet = false },
            title = stringResource(R.string.settings_font),
            subtitle = stringResource(R.string.settings_font_dialog_subtitle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppFont.entries.forEach { font ->
                    AppChoiceRow(
                        selected = appFont == font,
                        onClick = {
                            onSetAppFont(font)
                            showFontSheet = false
                        },
                        label = stringResource(font.labelRes)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showLanguageSheet) {
        AppModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            title = stringResource(R.string.settings_language),
            subtitle = stringResource(R.string.settings_language_dialog_title)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppChoiceRow(
                    selected = currentLocaleTag.isEmpty(),
                    onClick = {
                        onSetLanguage("")
                        showLanguageSheet = false
                    },
                    label = stringResource(R.string.settings_language_system_default)
                )

                supportedLocales.forEach { (tag, displayName) ->
                    AppChoiceRow(
                        selected = currentLocaleTag.equals(tag, ignoreCase = true),
                        onClick = {
                            onSetLanguage(tag)
                            showLanguageSheet = false
                        },
                        label = displayName
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showLeadTimeSheet) {
        AppModalBottomSheet(
            onDismissRequest = { showLeadTimeSheet = false },
            title = stringResource(R.string.notification_lead_time_title),
            subtitle = stringResource(R.string.settings_notification_lead_time_dialog_title)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotificationLeadTime.entries.forEach { leadTime ->
                    AppChoiceRow(
                        selected = notificationLeadTime == leadTime,
                        onClick = {
                            onSetNotificationLeadTime(leadTime)
                            showLeadTimeSheet = false
                        },
                        label = stringResource(leadTime.labelRes)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showSortOrderSheet) {
        AppModalBottomSheet(
            onDismissRequest = { showSortOrderSheet = false },
            title = stringResource(R.string.settings_sort_order),
            subtitle = stringResource(R.string.settings_sort_order_dialog_title)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortOrder.entries.forEach { order ->
                    AppChoiceRow(
                        selected = sortOrder == order,
                        onClick = {
                            onSetSortOrder(order)
                            showSortOrderSheet = false
                        },
                        label = stringResource(order.labelRes)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
