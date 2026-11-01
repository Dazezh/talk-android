/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.talk.ui.chat

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.nextcloud.talk.R
import com.nextcloud.talk.chat.ui.model.ChatMessageUi
import com.nextcloud.talk.chat.ui.model.MessageTypeContent
import com.nextcloud.talk.stickers.ui.StickerImage
import com.nextcloud.talk.utils.preferences.RalkAppearancePreferences

/**
 * Renders a message that carries nothing but a sticker link. The link has to be confirmed as a sticker by the
 * reference API of the server; while that is not the case the message is drawn as a regular link message.
 */
@Composable
fun StickerMessage(
    typeContent: MessageTypeContent.Sticker,
    message: ChatMessageUi,
    isOneToOneConversation: Boolean = false,
    conversationThreadId: Long? = null
) {
    // The sticker message already carries the direct image URL, which is served by the instance the
    // sticker lives on (the own server or, for federated stickers, the remote one).
    val stickerUrl = typeContent.url

    val context = LocalContext.current
    val stickerScale = remember(context) { RalkAppearancePreferences(context).stickerSize.scale }

    MessageScaffold(
        uiMessage = message,
        isOneToOneConversation = isOneToOneConversation,
        conversationThreadId = conversationThreadId,
        includePadding = false,
        forceTimeBelow = true,
        bubbleColor = Color.Transparent,
        bubbleShape = RectangleShape,
        content = {
            StickerImage(
                url = stickerUrl,
                authHeader = message.stickerAuthHeader(stickerUrl),
                contentDescription = stringResource(R.string.nc_sticker),
                modifier = Modifier.size(dimensionResource(R.dimen.sticker_message_size) * stickerScale)
            )
        }
    )
}

/**
 * The Authorization header is only attached for stickers of the own server; a sticker received through
 * federation is served by another instance which must not receive the own credentials.
 */
private fun ChatMessageUi.stickerAuthHeader(url: String): String? {
    val baseUrl = activeUserBaseUrl ?: return null
    return activeUserCredentials?.takeIf { url.startsWith(baseUrl) }
}
