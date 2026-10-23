/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.settings.chat

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import autodagger.AutoInjector
import com.nextcloud.talk.R
import com.nextcloud.talk.activities.BaseActivity
import com.nextcloud.talk.activities.MainActivity
import com.nextcloud.talk.application.NextcloudTalkApplication
import com.nextcloud.talk.chat.wallpaper.ChatWallpaper
import com.nextcloud.talk.chat.wallpaper.ChatWallpaperRepository
import com.nextcloud.talk.components.ColoredStatusBar
import com.nextcloud.talk.components.StandardAppBar
import com.nextcloud.talk.users.UserManager
import com.nextcloud.talk.utils.preferences.RalkAppearancePreferences
import com.nextcloud.talk.utils.preferences.StickerSize
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class ChatSettingsUiState(
    val rectangularInput: Boolean,
    val stickerSize: StickerSize,
    val accentSeed: Int,
    val wallpaper: ChatWallpaper?,
    val wallpaperOptions: List<ChatWallpaper>? = null,
    val isWallpaperLoading: Boolean = false
) {
    val isWallpaperPickerVisible: Boolean get() = wallpaperOptions != null || isWallpaperLoading
}

private data class ChatSettingsCallbacks(
    val onRectangularInputChange: (Boolean) -> Unit,
    val onStickerSizeChange: (StickerSize) -> Unit,
    val onAccentSelected: (Int) -> Unit,
    val onLoadWallpapers: () -> Unit,
    val onWallpaperSelected: (ChatWallpaper?) -> Unit,
    val onWallpaperPickerDismissed: () -> Unit
)

/**
 * Appearance settings that only affect the chat: the shape of the input field, the size of the
 * stickers, the wallpaper and the accent colour.
 */
@AutoInjector(NextcloudTalkApplication::class)
class ChatSettingsActivity : BaseActivity() {

    @Inject
    lateinit var chatWallpaperRepository: ChatWallpaperRepository

    @Inject
    lateinit var userManager: UserManager

    private var wallpaperOptions by mutableStateOf<List<ChatWallpaper>?>(null)
    private var isWallpaperLoading by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NextcloudTalkApplication.sharedApplication!!.componentApplication.inject(this)

        val colorScheme = viewThemeUtils.getColorScheme(this)

        setContent {
            val appearancePreferences = remember { RalkAppearancePreferences(this) }
            val wallpaperPath = appearancePreferences.wallpaperPath

            MaterialTheme(colorScheme = colorScheme) {
                ColoredStatusBar()
                Scaffold(
                    modifier = Modifier.statusBarsPadding(),
                    containerColor = colorResource(R.color.bg_default),
                    topBar = {
                        StandardAppBar(
                            title = stringResource(R.string.nc_settings_chat_settings_title),
                            menuItems = null
                        )
                    }
                ) { paddingValues ->
                    ChatSettingsScreen(
                        state = ChatSettingsUiState(
                            rectangularInput = appPreferences.messageInputRectangular,
                            stickerSize = appearancePreferences.stickerSize,
                            accentSeed = appearancePreferences.accentSeedOrDefault(this),
                            wallpaper = wallpaperPath?.let { path ->
                                ChatWallpaper(path = path, name = path.substringAfterLast('/'))
                            },
                            wallpaperOptions = wallpaperOptions,
                            isWallpaperLoading = isWallpaperLoading
                        ),
                        callbacks = buildCallbacks(appearancePreferences),
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    private fun buildCallbacks(appearancePreferences: RalkAppearancePreferences): ChatSettingsCallbacks =
        ChatSettingsCallbacks(
            onRectangularInputChange = { appPreferences.setMessageInputRectangular(it) },
            onStickerSizeChange = { appearancePreferences.stickerSize = it },
            onAccentSelected = ::applyAccent,
            onLoadWallpapers = ::loadWallpapers,
            onWallpaperSelected = ::applyWallpaper,
            onWallpaperPickerDismissed = { wallpaperOptions = null }
        )

    private fun applyAccent(seed: Int) {
        val appearancePreferences = RalkAppearancePreferences(this)
        if (appearancePreferences.accentSeedOrDefault(this) == seed) {
            return
        }

        appearancePreferences.accentSeed = seed
        restartApp()
    }

    private fun loadWallpapers() {
        val user = userManager.currentUser.blockingGet() ?: return
        isWallpaperLoading = true
        lifecycleScope.launch {
            wallpaperOptions = chatWallpaperRepository.listWallpapers(user)
            isWallpaperLoading = false
        }
    }

    private fun applyWallpaper(wallpaper: ChatWallpaper?) {
        wallpaperOptions = null
        val appearancePreferences = RalkAppearancePreferences(this)
        if (wallpaper == null) {
            appearancePreferences.clearWallpaper()
            return
        }

        appearancePreferences.wallpaperPath = wallpaper.path
        val user = userManager.currentUser.blockingGet() ?: return
        lifecycleScope.launch {
            chatWallpaperRepository.wallpaperFile(this@ChatSettingsActivity, user, wallpaper.path)
        }
    }

    /**
     * The colour scheme is read while views are inflated and composed, so the open screens keep the
     * old accent. Reopening the task is the simplest way to get every screen onto the new scheme.
     */
    private fun restartApp() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finishAffinity()
    }
}

@Composable
private fun ChatSettingsScreen(
    state: ChatSettingsUiState,
    callbacks: ChatSettingsCallbacks,
    modifier: Modifier = Modifier
) {
    var rectangularInput by remember(state.rectangularInput) { mutableStateOf(state.rectangularInput) }
    var stickerSize by remember(state.stickerSize) { mutableStateOf(state.stickerSize) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.nc_chat_settings_restart_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp)
        )

        SectionTitle(text = stringResource(R.string.nc_chat_settings_input_section))

        SwitchRow(
            title = stringResource(R.string.nc_settings_message_input_rectangular_title),
            checked = rectangularInput,
            onCheckedChange = {
                rectangularInput = it
                callbacks.onRectangularInputChange(it)
            }
        )

        SectionTitle(text = stringResource(R.string.nc_chat_settings_stickers_section))

        StickerSizeRow(
            selected = stickerSize,
            onSelect = {
                stickerSize = it
                callbacks.onStickerSizeChange(it)
            }
        )

        SectionTitle(text = stringResource(R.string.nc_chat_settings_wallpaper_section))

        WallpaperRow(
            wallpaperName = state.wallpaper?.name,
            onPick = callbacks.onLoadWallpapers
        )

        SectionTitle(text = stringResource(R.string.nc_chat_settings_accent_section))

        AccentRow(
            selectedAccent = state.accentSeed,
            onAccentSelected = callbacks.onAccentSelected
        )
    }

    if (state.isWallpaperPickerVisible) {
        WallpaperPickerDialog(
            options = state.wallpaperOptions,
            isLoading = state.isWallpaperLoading,
            onDismiss = callbacks.onWallpaperPickerDismissed,
            onSelect = callbacks.onWallpaperSelected
        )
    }
}

@Composable
private fun WallpaperRow(wallpaperName: String?, onPick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.nc_chat_settings_wallpaper_title),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        Text(
            text = wallpaperName ?: stringResource(R.string.nc_chat_settings_wallpaper_none),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onPick) {
            Text(stringResource(R.string.nc_chat_settings_wallpaper_pick))
        }
    }
}

@Composable
private fun StickerSizeRow(selected: StickerSize, onSelect: (StickerSize) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StickerSize.entries.forEach { size ->
            FilterChip(
                selected = size == selected,
                onClick = { onSelect(size) },
                label = { Text(size.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
private fun AccentRow(selectedAccent: Int, onAccentSelected: (Int) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        accentPresets.forEach { seed ->
            AccentSwatch(
                seed = seed,
                selected = seed == selectedAccent,
                onClick = { onAccentSelected(seed) }
            )
        }
        CustomAccentSwatch { showPicker = true }
    }

    if (showPicker) {
        AccentColorPickerDialog(
            initialSeed = selectedAccent,
            onDismiss = { showPicker = false },
            onApply = { seed ->
                showPicker = false
                onAccentSelected(seed)
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
