/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.stickers.data

import com.nextcloud.talk.models.json.opengraph.Reference
import com.nextcloud.talk.models.json.opengraph.RichObject
import com.nextcloud.talk.models.json.opengraph.RichObjectSticker
import com.nextcloud.talk.utils.ApiUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StickerReferenceTest {

    @Test
    fun `a confirmed sticker reference yields its image`() {
        val reference = reference(richObjectType = STICKER_TYPE, accessible = true, imageUrl = IMAGE_URL)

        assertEquals(IMAGE_URL, reference.stickerImageUrl())
    }

    @Test
    fun `a reference without the accessible flag is still accepted`() {
        val reference = reference(richObjectType = STICKER_TYPE, accessible = null, imageUrl = IMAGE_URL)

        assertEquals(IMAGE_URL, reference.stickerImageUrl())
    }

    @Test
    fun `an inaccessible reference is rejected`() {
        val reference = reference(richObjectType = STICKER_TYPE, accessible = false, imageUrl = IMAGE_URL)

        assertNull(reference.stickerImageUrl())
    }

    @Test
    fun `a reference of another rich object type is rejected`() {
        val reference = reference(richObjectType = "deck-card", accessible = true, imageUrl = IMAGE_URL)

        assertNull(reference.stickerImageUrl())
    }

    @Test
    fun `the thumbnail is used when the reference carries no full image`() {
        val reference = reference(richObjectType = STICKER_TYPE, accessible = true, thumbnailUrl = THUMBNAIL_URL)

        assertEquals(THUMBNAIL_URL, reference.stickerImageUrl())
    }

    @Test
    fun `only urls of the sticker image route count as stickers`() {
        assertTrue(ApiUtils.isStickerImageUrl(IMAGE_URL))
        assertTrue(ApiUtils.isStickerImageUrl("https://other.example.com/apps/r-stiker/s/bW9jaGljYXQ6"))
        assertFalse(ApiUtils.isStickerImageUrl("https://cloud.example.com/apps/r-stiker/api/v1/packs"))
        assertFalse(ApiUtils.isStickerImageUrl("https://example.com/index.php/s/abcdef"))
    }

    private fun reference(
        richObjectType: String,
        accessible: Boolean?,
        imageUrl: String? = null,
        thumbnailUrl: String? = null
    ) = Reference(
        richObjectType = richObjectType,
        richObject = RichObject(
            id = "bW9jaGljYXQ6MC53ZWJw",
            name = "Mochi Cat",
            sticker = RichObjectSticker(imageUrl = imageUrl, thumbnailUrl = thumbnailUrl)
        ),
        openGraphObject = null,
        accessible = accessible
    )

    companion object {
        private const val STICKER_TYPE = "r-stiker"
        private const val IMAGE_URL = "https://cloud.example.com/apps/r-stiker/s/bW9jaGljYXQ6MC53ZWJw"
        private const val THUMBNAIL_URL = "https://cloud.example.com/apps/r-stiker/s/thumbnail"
    }
}
