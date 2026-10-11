/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.data.model

/**
 * One page of stickers of a pack. [nextCursor] is the offset of the following page, null when the pack
 * has been fully loaded.
 */
data class StickerPage(val stickers: List<Sticker>, val nextCursor: Int?, val total: Int)
