/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nextcloud.talk.R
import com.nextcloud.talk.stickers.data.model.Sticker
import com.nextcloud.talk.stickers.data.model.StickerPack
import com.nextcloud.talk.stickers.viewmodels.StickersViewModel.Companion.TAB_EMOJI
import com.nextcloud.talk.stickers.viewmodels.StickersViewModel.StickerPickerState

private const val LOAD_MORE_THRESHOLD = 8
private val tabIconSize = 24.dp
private val stickerCellMinSize = 64.dp
private val packTabMaxWidth = 140.dp

/**
 * Horizontal tab strip of the picker: emoji first, then one tab per sticker pack, following the layout
 * Telegram uses for its sticker packs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerPickerTabs(
    packs: List<StickerPack>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    PrimaryScrollableTabRow(
        selectedTabIndex = selectedTab.coerceIn(0, packs.size),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        edgePadding = 0.dp,
        modifier = modifier
    ) {
        Tab(
            selected = selectedTab == TAB_EMOJI,
            onClick = { onTabSelected(TAB_EMOJI) },
            text = {
                Icon(
                    painter = painterResource(R.drawable.ic_insert_emoticon_black_24dp),
                    contentDescription = stringResource(R.string.nc_add_emojis),
                    modifier = Modifier.size(tabIconSize)
                )
            }
        )
        packs.forEachIndexed { index, pack ->
            Tab(
                selected = selectedTab == index + 1,
                onClick = { onTabSelected(index + 1) },
                text = {
                    Text(
                        text = pack.displayName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = packTabMaxWidth)
                    )
                }
            )
        }
    }
}

/**
 * Lazily loaded sticker grid of the selected pack. Stickers are drawn without a background so their
 * transparency is kept.
 */
@Composable
fun StickerPickerGrid(
    state: StickerPickerState,
    onLoadMore: () -> Unit,
    onStickerClick: (Sticker) -> Unit,
    modifier: Modifier = Modifier
) {
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
                StickerCell(sticker, state.credentials, onStickerClick)
            }
        }

        if (state.isLoadingStickers) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun StickerCell(sticker: Sticker, credentials: String, onStickerClick: (Sticker) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.small)
            .clickable { onStickerClick(sticker) },
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
