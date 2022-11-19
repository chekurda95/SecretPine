package com.chekurda.secret_pine.main_screen.data

import com.chekurda.common.base_list.ComparableItem
import java.util.UUID

internal data class Message(
    val uuid: UUID,
    val senderName: String,
    val text: String,
    val isOutgoing: Boolean
) : ComparableItem<Message> {

    override fun areItemsTheSame(anotherItem: Message): Boolean =
        uuid == anotherItem.uuid

    override fun areContentsTheSame(anotherItem: Message): Boolean =
        this == anotherItem
}