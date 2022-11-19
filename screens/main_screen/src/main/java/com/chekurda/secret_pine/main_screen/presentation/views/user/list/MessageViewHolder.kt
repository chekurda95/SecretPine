package com.chekurda.secret_pine.main_screen.presentation.views.user.list

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import com.chekurda.secret_pine.main_screen.data.Message
import com.chekurda.secret_pine.main_screen.presentation.views.user.MessageItemView

/**
 * ViewHolder списка сообщений.
 */
internal class MessageViewHolder private constructor(
    private val view: MessageItemView
) : RecyclerView.ViewHolder(view) {

    constructor(context: Context) : this(MessageItemView(context))

    private lateinit var data: Message

    fun bind(data: Message) {
        this.data = data
        view.setData(data)
    }
}