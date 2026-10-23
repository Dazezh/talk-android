/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nextcloud.talk.R
import com.nextcloud.talk.stickers.data.model.Sticker
import com.nextcloud.talk.stickers.viewmodels.StickerTab
import com.nextcloud.talk.stickers.viewmodels.StickersViewModel.StickerPickerState

private const val LOAD_MORE_THRESHOLD = 8
private val tabIconSize = 24.dp
private val stickerCellMinSize = 64.dp
private val packTabMaxWidth = 140.dp

/**
 * Horizontal tab strip of the picker: emoji first, then the favorites and the recently sent stickers as long
 * as they have entries, followed by one tab per sticker pack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPickerTabs(
    tabs: List<StickerTab>,
    selectedTab: StickerTab,
    onTabSelected: (StickerTab) -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = tabs.indexOf(selectedTab).coerceIn(0, tabs.lastIndex.coerceAtLeast(0)),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        edgePadding = 0.dp,
        modifier = modifier
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                text = { StickerTabLabel(tab) }
            )
        }
    }
}

@Composable
private fun StickerTabLabel(tab: StickerTab) {
    when (tab) {
        StickerTab.Emoji -> StickerTabIcon(R.drawable.ic_insert_emoticon_black_24dp, R.string.nc_add_emojis)
        StickerTab.Favorites -> StickerTabIcon(R.drawable.ic_star_black_24dp, R.string.nc_stickers_favorites)
        StickerTab.Recent -> StickerTabIcon(R.drawable.outline_access_time_24, R.string.nc_stickers_recent)
        is StickerTab.Pack -> Text(
            text = tab.displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = packTabMaxWidth)
        )
    }
}

@Composable
private fun StickerTabIcon(@DrawableRes iconRes: Int, @StringRes contentDescriptionRes: Int) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = stringResource(contentDescriptionRes),
        modifier = Modifier.size(tabIconSize)
    )
}

/**
 * Lazily loaded sticker grid of the selected tab. Stickers are drawn without a background so their
 * transparency is kept. A long press offers to add the sticker to the favorites or to remove it from them.
 */
@Composable
fun StickerPickerGrid(
    state: StickerPickerState,
    onLoadMore: () -> Unit,
    onStickerClick: (Sticker) -> Unit,
    onFavoriteToggle: (Sticker, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var favoriteDialogSticker by remember { mutableStateOf<Sticker?>(null) }
    val gridState = rememberLazyGridState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= gridState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore, state.nextCursor) {
        if (shouldLoadMore && state.nextCursor != null) onLoadMore()
    }

    Box(modifier = modifier) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = stickerCellMinSize),
            contentPadding = PaddingValues(dimensionResource(R.dimen.standard_half_margin)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.standard_half_margin)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.standard_half_margin)),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.stickers, key = { it.id }) { sticker ->
                StickerCell(sticker, state.credentials, onStickerClick) { favoriteDialogSticker = sticker }
            }
        }

        if (state.isLoadingStickers) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }

    favoriteDialogSticker?.let { sticker ->
        StickerFavoriteDialog(
            sticker = sticker,
            isFavorite = sticker.id in state.favoriteIds,
            credentials = state.credentials,
            onConfirm = {
                onFavoriteToggle(sticker, sticker.id !in state.favoriteIds)
                favoriteDialogSticker = null
            },
            onDismiss = { favoriteDialogSticker = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun StickerCell(
    sticker: Sticker,
    credentials: String,
    onStickerClick: (Sticker) -> Unit,
    onStickerLongClick: (Sticker) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(
                onClick = { onStickerClick(sticker) },
                onLongClick = { onStickerLongClick(sticker) }
            ),
        contentAlignment = Alignment.Center
    ) {
        StickerImage(
            url = sticker.thumbnailUrl,
            authHeader = credentials,
            contentDescription = sticker.title,
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.standard_quarter_margin))
        )
    }
}

@Composable
private fun StickerFavoriteDialog(
    sticker: Sticker,
    isFavorite: Boolean,
    credentials: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val actionRes = if (isFavorite) R.string.nc_sticker_remove_favorite else R.string.nc_sticker_add_favorite
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = sticker.title) },
        text = {
            StickerImage(
                url = sticker.thumbnailUrl,
                authHeader = credentials,
                contentDescription = sticker.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensionResource(R.dimen.sticker_message_size))
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(text = stringResource(actionRes)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.nc_cancel)) }
        }
    )
}
