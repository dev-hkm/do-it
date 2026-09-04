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

package at.co.schwaerzler.maximilian.doit.data

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import at.co.schwaerzler.maximilian.doit.DoItApplication
import at.co.schwaerzler.maximilian.doit.R
import at.co.schwaerzler.maximilian.doit.data.backup.TodoBackup
import at.co.schwaerzler.maximilian.doit.data.backup.CloudBackupClient
import at.co.schwaerzler.maximilian.doit.data.db.TodoRepository
import at.co.schwaerzler.maximilian.doit.util.AppThemeMode
import at.co.schwaerzler.maximilian.doit.util.AppFont
import at.co.schwaerzler.maximilian.doit.util.NotificationLeadTime
import at.co.schwaerzler.maximilian.doit.util.SortOrder
import at.co.schwaerzler.maximilian.doit.util.applyNightMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import androidx.lifecycle.viewModelScope
import at.co.schwaerzler.maximilian.doit.data.db.entity.Tag
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** ViewModel for the settings screen, owning theme preference, locale, security, and tags state. */
class SettingsViewModel(
    private val application: Application,
    private val appPreferences: AppPreferences,
    private val repository: TodoRepository
) : ViewModel() {
    val themeMode = appPreferences.themeMode
    val notificationLeadTime = appPreferences.notificationLeadTime
    val sortOrder = appPreferences.homeScreenSortOrder
    val tagsEnabled = appPreferences.tagsEnabled
    val appFont = appPreferences.appFont
    val cloudBackupToken = appPreferences.cloudBackupToken
    val useDynamicColors = appPreferences.useDynamicColors
    val appLockEnabled = appPreferences.appLockEnabled
    val appLockPin = appPreferences.appLockPin
    val biometricUnlockEnabled = appPreferences.biometricUnlockEnabled

    val allTags = repository.getAllTagsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val versionName: String? =
        application.packageManager.getPackageInfo(application.packageName, 0).versionName
    private val _currentLocaleTag = MutableStateFlow(run {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) "" else locales.toLanguageTags().split(",").first().trim()
    })
    val currentLocaleTag: StateFlow<String> = _currentLocaleTag.asStateFlow()
    val supportedLocales: List<Pair<String, String>> =
        application.resources.getStringArray(R.array.supported_locales).map { tag ->
            val locale = Locale.forLanguageTag(tag)
            tag to locale.getDisplayName(locale).replaceFirstChar { it.uppercaseChar() }
        }

    fun setAppLockEnabled(enabled: Boolean) = appPreferences.saveAppLockEnabled(enabled)
    fun setAppLockPin(pin: String?) = appPreferences.saveAppLockPin(pin)
    fun setBiometricUnlockEnabled(enabled: Boolean) = appPreferences.saveBiometricUnlockEnabled(enabled)

    fun createTag(tag: Tag) = viewModelScope.launch { repository.insertTag(tag) }
    fun updateTag(tag: Tag) = viewModelScope.launch { repository.updateTag(tag) }
    fun deleteTag(tag: Tag) = viewModelScope.launch { repository.deleteTag(tag) }

    fun setTheme(mode: AppThemeMode) {
        mode.applyNightMode(application)
        appPreferences.saveThemeMode(mode)
    }

    fun setNotificationLeadTime(leadTime: NotificationLeadTime) {
        appPreferences.saveNotificationLeadTime(leadTime)
    }

    fun setSortOrder(order: SortOrder) = appPreferences.saveHomeScreenSortOrder(order)

    fun setTagsEnabled(enabled: Boolean) = appPreferences.saveTagsEnabled(enabled)

    fun setAppFont(font: AppFont) = appPreferences.saveAppFont(font)

    fun createCloudToken(): String {
        val token = CloudBackupClient.newToken()
        appPreferences.saveCloudBackupToken(token)
        return token
    }

    fun setCloudToken(token: String) = appPreferences.saveCloudBackupToken(token)

    suspend fun uploadCloudBackup(token: String): Result<Unit> = runCatching {
        require(token.matches(Regex("hkm-[A-Za-z0-9]{36}"))) { "Enter a valid HKM token" }
        CloudBackupClient.upload(token, TodoBackup.encode(repository.getAllTodos()))
    }

    suspend fun restoreCloudBackup(token: String): Result<Int> = runCatching {
        require(token.matches(Regex("hkm-[A-Za-z0-9]{36}"))) { "Enter a valid HKM token" }
        val todos = TodoBackup.decode(CloudBackupClient.restore(token))
        repository.replaceAllTodos(todos)
        todos.size
    }

    fun setUseDynamicColor(use: Boolean) {
        appPreferences.saveUseDynamicColors(use)
    }

    fun setLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(tag)
        )
        _currentLocaleTag.value = tag
    }

    suspend fun exportTodosJson(): String = TodoBackup.encode(repository.getAllTodos())

    suspend fun importTodosJson(json: String): Int {
        val todos = TodoBackup.decode(json)
        repository.replaceAllTodos(todos)
        return todos.size
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DoItApplication
                SettingsViewModel(
                    application = app,
                    appPreferences = app.appPreferences,
                    repository = app.repository
                )
            }
        }
    }
}
