/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.nextcloud.talk.settings.chat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
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
import com.nextcloud.talk.chat.wallpaper.loadWallpaperImage
import com.nextcloud.talk.components.ColoredStatusBar
import com.nextcloud.talk.components.StandardAppBar
import com.nextcloud.talk.data.user.model.User
import com.nextcloud.talk.users.UserManager
import com.nextcloud.talk.utils.preferences.RalkAppearancePreferences
import com.nextcloud.talk.utils.preferences.StickerSize
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val THUMBNAIL_MAX_DIMENSION = 320
private const val NO_WALLPAPER_KEY = "no_wallpaper"
private const val UPLOAD_KEY = "upload_wallpaper"
private const val LOADING_KEY = "wallpaper_loading"
private const val DEFAULT_WALLPAPER_NAME = "wallpaper.jpg"
private const val FIRST_DUPLICATE_INDEX = 2
private const val WALLPAPER_MIME_TYPE = "image/*"
private const val TILE_BORDER_WIDTH = 2
private val tileSize = 84.dp
private val tileShape = RoundedCornerShape(12.dp)
private val tilePadding = 16.dp

private data class ChatSettingsUiState(
    val rectangularInput: Boolean,
    val stickerSize: StickerSize,
    val accentSeed: Int,
    val wallpaperList: WallpaperListState
)

private data class ChatSettingsCallbacks(
    val onRectangularInputChange: (Boolean) -> Unit,
    val onStickerSizeChange: (StickerSize) -> Unit,
    val onAccentSelected: (Int) -> Unit,
    val onWallpaperSelected: (String?) -> Unit,
    val onUpload: () -> Unit,
    val onApply: () -> Unit
)

private data class WallpaperListState(
    val wallpapers: List<ChatWallpaper>?,
    val selectedPath: String?,
    val isLoading: Boolean,
    val isUploading: Boolean
)

/**
 * Appearance settings that only affect the chat: the shape of the input field, the size of the
 * stickers, the accent colour and the wallpaper. Everything is staged locally and written to the
 * preferences only when the user confirms with the apply button.
 */
@AutoInjector(NextcloudTalkApplication::class)
class ChatSettingsActivity : BaseActivity() {

    @Inject
    lateinit var chatWallpaperRepository: ChatWallpaperRepository

    @Inject
    lateinit var userManager: UserManager

    private var rectangularInput by mutableStateOf(false)
    private var stickerSize by mutableStateOf(StickerSize.MEDIUM)
    private var accentSeed by mutableStateOf(0)
    private var wallpaperPath by mutableStateOf<String?>(null)
    private var wallpapers by mutableStateOf<List<ChatWallpaper>?>(null)
    private var isWallpaperLoading by mutableStateOf(false)
    private var isUploading by mutableStateOf(false)

    private var wallpaperUser: User? = null
    private lateinit var pickWallpaper: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NextcloudTalkApplication.sharedApplication!!.componentApplication.inject(this)

        pickWallpaper = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                uploadWallpaper(uri)
            }
        }

        val appearancePreferences = RalkAppearancePreferences(this)
        rectangularInput = appPreferences.messageInputRectangular
        stickerSize = appearancePreferences.stickerSize
        accentSeed = appearancePreferences.accentSeedOrDefault(this)
        wallpaperPath = appearancePreferences.wallpaperPath
        wallpaperUser = userManager.currentUser.blockingGet()
        loadWallpapers()

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
                    },
                    bottomBar = {
                        Surface(color = colorResource(R.color.bg_default)) {
                            Button(
                                onClick = { applySettings() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = tilePadding, vertical = 12.dp),
                                shape = tileShape
                            ) {
                                Text(stringResource(R.string.nc_chat_settings_apply))
                            }
                        }
                    }
                ) { paddingValues ->
                    ChatSettingsScreen(
                        state = ChatSettingsUiState(
                            rectangularInput = rectangularInput,
                            stickerSize = stickerSize,
                            accentSeed = accentSeed,
                            wallpaperList = WallpaperListState(
                                wallpapers = wallpapers,
                                selectedPath = wallpaperPath,
                                isLoading = isWallpaperLoading,
                                isUploading = isUploading
                            )
                        ),
                        callbacks = buildCallbacks(),
                        repository = chatWallpaperRepository,
                        user = wallpaperUser,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    private fun buildCallbacks(): ChatSettingsCallbacks =
        ChatSettingsCallbacks(
            onRectangularInputChange = { rectangularInput = it },
            onStickerSizeChange = { stickerSize = it },
            onAccentSelected = { accentSeed = it },
            onWallpaperSelected = { wallpaperPath = it },
            onUpload = { pickWallpaper.launch(WALLPAPER_MIME_TYPE) },
            onApply = ::applySettings
        )

    private fun loadWallpapers() {
        val user = wallpaperUser ?: return
        isWallpaperLoading = true
        lifecycleScope.launch {
            wallpapers = chatWallpaperRepository.listWallpapers(user)
            isWallpaperLoading = false
        }
    }

    private fun uploadWallpaper(source: Uri) {
        val user = wallpaperUser ?: return
        isUploading = true
        lifecycleScope.launch {
            val uploaded = chatWallpaperRepository.uploadWallpaper(
                context = this@ChatSettingsActivity,
                user = user,
                source = source,
                fileName = uniqueFileName(source)
            )
            isUploading = false
            if (uploaded == null) {
                Toast.makeText(this@ChatSettingsActivity, R.string.nc_common_error_sorry, Toast.LENGTH_LONG).show()
            } else {
                wallpaperPath = uploaded.path
                loadWallpapers()
            }
        }
    }

    /** Keeps existing wallpapers intact by numbering a second upload with the same name. */
    private fun uniqueFileName(source: Uri): String {
        val displayName = displayName(source) ?: DEFAULT_WALLPAPER_NAME
        val taken = wallpapers.orEmpty().mapTo(mutableSetOf()) { it.name }
        if (displayName !in taken) {
            return displayName
        }

        val base = displayName.substringBeforeLast('.')
        val extension = displayName.substringAfterLast('.', "")
        var index = FIRST_DUPLICATE_INDEX
        var candidate: String
        do {
            candidate = if (extension.isEmpty()) "$base ($index)" else "$base ($index).$extension"
            index++
        } while (candidate in taken)

        return candidate
    }

    private fun displayName(source: Uri): String? =
        contentResolver.query(source, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }

    private fun applySettings() {
        appPreferences.setMessageInputRectangular(rectangularInput)
        val appearancePreferences = RalkAppearancePreferences(this)
        val accentChanged = appearancePreferences.accentSeedOrDefault(this) != accentSeed

        appearancePreferences.stickerSize = stickerSize
        appearancePreferences.accentSeed = accentSeed
        if (wallpaperPath == null) {
            appearancePreferences.clearWallpaper()
        } else {
            appearancePreferences.wallpaperPath = wallpaperPath
        }

        if (accentChanged) {
            restartApp()
        } else {
            finish()
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
    repository: ChatWallpaperRepository,
    user: User?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = stringResource(R.string.nc_chat_settings_restart_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = tilePadding, top = 12.dp, end = tilePadding)
        )

        SectionTitle(textRes = R.string.nc_chat_settings_input_section)

        SwitchRow(
            title = stringResource(R.string.nc_settings_message_input_rectangular_title),
            checked = state.rectangularInput,
            onCheckedChange = callbacks.onRectangularInputChange
        )

        SectionTitle(textRes = R.string.nc_chat_settings_stickers_section)

        StickerSizeRow(selected = state.stickerSize, onSelect = callbacks.onStickerSizeChange)

        SectionTitle(textRes = R.string.nc_chat_settings_accent_section)

        AccentRow(selectedAccent = state.accentSeed, onAccentSelected = callbacks.onAccentSelected)

        SectionTitle(textRes = R.string.nc_chat_settings_wallpaper_section)

        WallpaperCarousel(
            state = state.wallpaperList,
            repository = repository,
            user = user,
            onSelect = callbacks.onWallpaperSelected,
            onUpload = callbacks.onUpload
        )

        SectionTitle(textRes = R.string.nc_chat_settings_preview_section)

        ChatPreview(
            state = ChatPreviewState(
                wallpaperPath = state.wallpaperList.selectedPath,
                accentSeed = state.accentSeed,
                stickerSize = state.stickerSize
            ),
            repository = repository,
            user = user
        )
    }
}

@Composable
private fun WallpaperCarousel(
    state: WallpaperListState,
    repository: ChatWallpaperRepository,
    user: User?,
    onSelect: (String?) -> Unit,
    onUpload: () -> Unit
) {
    val wallpapers = state.wallpapers

    LazyRow(
        contentPadding = PaddingValues(horizontal = tilePadding, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = UPLOAD_KEY) {
            UploadTile(isUploading = state.isUploading, onClick = onUpload)
        }
        item(key = NO_WALLPAPER_KEY) {
            WallpaperTile(
                option = null,
                selected = state.selectedPath == null,
                repository = repository,
                user = user,
                onSelect = { onSelect(null) }
            )
        }
        if (wallpapers == null) {
            item(key = LOADING_KEY) { LoadingTile() }
        } else {
            items(items = wallpapers, key = { it.path }) { wallpaper ->
                WallpaperTile(
                    option = wallpaper,
                    selected = state.selectedPath == wallpaper.path,
                    repository = repository,
                    user = user,
                    onSelect = { onSelect(wallpaper.path) }
                )
            }
        }
    }

    if (wallpapers != null && wallpapers.isEmpty() && !state.isLoading) {
        Text(
            text = stringResource(R.string.nc_chat_settings_wallpaper_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = tilePadding, vertical = 8.dp)
        )
    }
}

@Composable
private fun WallpaperTile(
    option: ChatWallpaper?,
    selected: Boolean,
    repository: ChatWallpaperRepository,
    user: User?,
    onSelect: () -> Unit
) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(initialValue = null, option?.path, user) {
        value = null
        val path = option?.path
        if (path != null && user != null) {
            value = repository.wallpaperFile(context, user, path)?.let {
                loadWallpaperImage(it, THUMBNAIL_MAX_DIMENSION)
            }
        }
    }

    Box(
        modifier = Modifier
            .size(tileSize)
            .clip(tileShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = TILE_BORDER_WIDTH.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = tileShape
            )
            .clickable(onClick = onSelect),
        contentAlignment = Alignment.Center
    ) {
        if (option == null) {
            Text(
                text = stringResource(R.string.nc_chat_settings_wallpaper_clear),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        } else {
            image?.let {
                Image(
                    bitmap = it,
                    contentDescription = option.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun UploadTile(isUploading: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(tileSize)
            .clip(tileShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = !isUploading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isUploading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.upload),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.nc_chat_settings_wallpaper_upload),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingTile() {
    Box(modifier = Modifier.size(tileSize), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun StickerSizeRow(selected: StickerSize, onSelect: (StickerSize) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = tilePadding, vertical = 8.dp),
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
            .padding(horizontal = tilePadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        accentPresets.forEach { seed ->
            AccentSwatch(seed = seed, selected = seed == selectedAccent, onClick = { onAccentSelected(seed) })
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
private fun SectionTitle(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = tilePadding, top = tilePadding, bottom = 4.dp)
    )
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = tilePadding, vertical = 12.dp),
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
