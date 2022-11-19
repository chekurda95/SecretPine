package com.chekurda.secret_pine.main_screen.presentation.views.user.list

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.chekurda.common.base_list.calculateDiff
import com.chekurda.secret_pine.main_screen.data.Message

internal class MessageListAdapter : RecyclerView.Adapter<MessageViewHolder>() {

    var messageList: List<Message> = emptyList()
        private set

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        recyclerView.recycledViewPool.setMaxRecycledViews(MESSAGE_VIEW_HOLDER_TYPE, 100)
    }

    fun setDataList(dataList: List<Message>) {
        val diffResult = calculateDiff(messageList, dataList)
        messageList = dataList
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder =
        when (viewType) {
            MESSAGE_VIEW_HOLDER_TYPE -> MessageViewHolder(parent.context)
            else -> throw IllegalArgumentException("Unsupported view holder type $viewType")
        }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messageList[position])
    }

    override fun getItemCount(): Int = messageList.size
    override fun getItemViewType(position: Int): Int = MESSAGE_VIEW_HOLDER_TYPE
}

private const val MESSAGE_VIEW_HOLDER_TYPE = 1