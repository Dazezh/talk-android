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
import autodagger.AutoInjector
import com.nextcloud.talk.R
import com.nextcloud.talk.activities.BaseActivity
import com.nextcloud.talk.activities.MainActivity
import com.nextcloud.talk.application.NextcloudTalkApplication
import com.nextcloud.talk.components.ColoredStatusBar
import com.nextcloud.talk.components.StandardAppBar
import com.nextcloud.talk.utils.preferences.RalkAppearancePreferences
import com.nextcloud.talk.utils.preferences.StickerSize

/**
 * Appearance settings that only affect the chat: the shape of the input field, the size of the
 * stickers, the wallpaper and the accent colour.
 */
@AutoInjector(NextcloudTalkApplication::class)
class ChatSettingsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NextcloudTalkApplication.sharedApplication!!.componentApplication.inject(this)

        val colorScheme = viewThemeUtils.getColorScheme(this)
        val appearancePreferences = RalkAppearancePreferences(this)

        setContent {
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
                        initialRectangularInput = appPreferences.messageInputRectangular,
                        onRectangularInputChange = { appPreferences.setMessageInputRectangular(it) },
                        initialStickerSize = appearancePreferences.stickerSize,
                        onStickerSizeChange = { appearancePreferences.stickerSize = it },
                        selectedAccent = appearancePreferences.accentSeedOrDefault(this),
                        onAccentSelected = ::applyAccent,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    private fun applyAccent(seed: Int) {
        val appearancePreferences = RalkAppearancePreferences(this)
        if (appearancePreferences.accentSeedOrDefault(this) == seed) {
            return
        }

        appearancePreferences.accentSeed = seed
        restartApp()
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
    initialRectangularInput: Boolean,
    onRectangularInputChange: (Boolean) -> Unit,
    initialStickerSize: StickerSize,
    onStickerSizeChange: (StickerSize) -> Unit,
    selectedAccent: Int,
    onAccentSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var rectangularInput by remember(initialRectangularInput) { mutableStateOf(initialRectangularInput) }
    var stickerSize by remember(initialStickerSize) { mutableStateOf(initialStickerSize) }

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
                onRectangularInputChange(it)
            }
        )

        SectionTitle(text = stringResource(R.string.nc_chat_settings_stickers_section))

        StickerSizeRow(
            selected = stickerSize,
            onSelect = {
                stickerSize = it
                onStickerSizeChange(it)
            }
        )

        SectionTitle(text = stringResource(R.string.nc_chat_settings_accent_section))

        AccentRow(selectedAccent = selectedAccent, onAccentSelected = onAccentSelected)
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
