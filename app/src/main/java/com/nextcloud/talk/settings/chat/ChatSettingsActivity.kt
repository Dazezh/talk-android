/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.settings.chat

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.nextcloud.talk.application.NextcloudTalkApplication
import com.nextcloud.talk.components.ColoredStatusBar
import com.nextcloud.talk.components.StandardAppBar
import com.nextcloud.talk.ui.theme.ViewThemeUtils
import com.nextcloud.talk.utils.preferences.AppPreferences
import javax.inject.Inject

/**
 * Appearance settings that only affect the chat: the shape of the input field, the size of the
 * stickers, the wallpaper and the accent colour.
 */
@AutoInjector(NextcloudTalkApplication::class)
class ChatSettingsActivity : BaseActivity() {

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NextcloudTalkApplication.sharedApplication!!.componentApplication.inject(this)

        val colorScheme = viewThemeUtils.getColorScheme(this)

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
                        modifier = Modifier.padding(paddingValues),
                        initialRectangularInput = appPreferences.messageInputRectangular,
                        onRectangularInputChange = { appPreferences.setMessageInputRectangular(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatSettingsScreen(
    initialRectangularInput: Boolean,
    onRectangularInputChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var rectangularInput by remember(initialRectangularInput) { mutableStateOf(initialRectangularInput) }

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
