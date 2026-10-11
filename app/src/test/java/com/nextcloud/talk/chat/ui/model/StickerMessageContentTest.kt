/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.chat.ui.model

import com.nextcloud.talk.chat.data.model.ChatMessage
import com.nextcloud.talk.data.user.model.User
import com.nextcloud.talk.models.json.capabilities.Capabilities
import com.nextcloud.talk.models.json.capabilities.CoreCapability
import com.nextcloud.talk.models.json.capabilities.RStikerCapability
import com.nextcloud.talk.models.json.capabilities.StickersCapability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StickerMessageContentTest {

    @Test
    fun `a message with a sticker link and sticker support becomes a sticker`() {
        val message = message(STICKER_URL)

        val content = getMessageTypeContent(userWithStickers(), message)

        assertTrue(content is MessageTypeContent.Sticker)
        assertEquals(STICKER_URL, (content as MessageTypeContent.Sticker).url)
    }

    @Test
    fun `without sticker support the same message stays a link preview`() {
        val content = getMessageTypeContent(userWithStickers(stickers = false), message(STICKER_URL))

        assertTrue(content is MessageTypeContent.LinkPreview)
    }

    @Test
    fun `a message with additional text is not turned into a sticker`() {
        val content = getMessageTypeContent(userWithStickers(), message("look at this $STICKER_URL"))

        assertTrue(content is MessageTypeContent.LinkPreview)
    }

    @Test
    fun `a link of another app stays a link preview`() {
        val content = getMessageTypeContent(userWithStickers(), message("https://cloud.example.com/index.php/s/abc"))

        assertTrue(content is MessageTypeContent.LinkPreview)
    }

    @Test
    fun `a message without a link stays regular text`() {
        assertEquals(MessageTypeContent.RegularText, getMessageTypeContent(userWithStickers(), message("hello")))
    }

    private fun message(text: String) = ChatMessage().apply { message = text }

    private fun userWithStickers(stickers: Boolean = true) =
        User(
            id = 1L,
            userId = "alice",
            username = "alice",
            baseUrl = "https://cloud.example.com",
            token = "token",
            capabilities = Capabilities(
                coreCapability = CoreCapability(null, null, "true", null, null),
                spreedCapability = null,
                notificationsCapability = null,
                themingCapability = null,
                externalCapability = null,
                provisioningCapability = null,
                userStatusCapability = null,
                passwordPolicy = null,
                rStikerCapability = if (stickers) {
                    RStikerCapability(1, 1, StickersCapability(null, true), false)
                } else {
                    null
                }
            )
        )

    companion object {
        private const val STICKER_URL = "https://cloud.example.com/apps/r-stiker/s/bW9jaGljYXQ6MC53ZWJw"
    }
}
