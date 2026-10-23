/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.viewmodels

import com.nextcloud.talk.stickers.data.model.StickerPack

/**
 * One entry of the sticker tab strip: the emoji picker, the locally stored favorites and recently sent
 * stickers and one tab per sticker pack of the server.
 */
sealed interface StickerTab {
    object Emoji : StickerTab
    object Favorites : StickerTab
    object Recent : StickerTab
    data class Pack(val name: String, val displayName: String) : StickerTab

    companion object {
        /** The tabs in display order; the favorites and recent tabs only exist while they have entries. */
        fun of(packs: List<StickerPack>, hasFavorites: Boolean, hasRecent: Boolean): List<StickerTab> =
            buildList {
                add(Emoji)
                if (hasFavorites) add(Favorites)
                if (hasRecent) add(Recent)
                packs.forEach { add(Pack(it.name, it.displayName)) }
            }
    }
}
