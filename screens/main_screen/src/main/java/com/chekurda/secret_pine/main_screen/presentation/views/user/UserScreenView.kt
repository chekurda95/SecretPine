package com.chekurda.secret_pine.main_screen.presentation.views.user

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chekurda.common.half
import com.chekurda.design.custom_view_tools.utils.MeasureSpecUtils.makeExactlySpec
import com.chekurda.design.custom_view_tools.utils.MeasureSpecUtils.makeUnspecifiedSpec
import com.chekurda.design.custom_view_tools.utils.MeasureSpecUtils.measureDirection
import com.chekurda.design.custom_view_tools.utils.dp
import com.chekurda.design.custom_view_tools.utils.layout
import com.chekurda.secret_pine.main_screen.data.Message
import com.chekurda.secret_pine.main_screen.presentation.views.ConnectionStateView
import com.chekurda.secret_pine.main_screen.presentation.views.user.list.MessageListAdapter
import com.chekurda.secret_pine.main_screen.presentation.views.user.panel.MessagePanel
import com.chekurda.secret_pine.main_screen.presentation.views.user.panel.MessagePanelController
import org.apache.commons.lang3.StringUtils

internal class UserScreenView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    private val adapter = MessageListAdapter()

    private val messagePanel = MessagePanel(context)
    private val messageListView = RecyclerView(context).apply {
        adapter = this@UserScreenView.adapter
        layoutManager = object : LinearLayoutManager(context) {
            override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {
                kotlin.runCatching { super.onLayoutChildren(recycler, state) }
            }
        }
        clipToPadding = false
    }
    private val connectionSpacingVertical = dp(15)
    private val connectionStateView = ConnectionStateView(context).apply {
        state = ConnectionStateView.State.SEARCH_PINE
        updatePadding(left = dp(40), right = dp(40), top = dp(40), bottom = dp(40))
    }
    private lateinit var controller: MessagePanelController

    var state: ConnectionStateView.State
        get() = connectionStateView.state
        set(value) {
            connectionStateView.state = value
        }

    fun attachController(controller: MessagePanelController) {
        this.controller = controller
    }

    fun updateConnectionState(isConnected: Boolean) {
        messagePanel.isEnabled = isConnected
        state = if (isConnected) ConnectionStateView.State.CONNECTED else ConnectionStateView.State.SEARCH_PINE
    }

    fun updateMessageList(messageList: List<Message>) {
        adapter.setDataList(messageList)
        Log.e("TAGTAG", "updateMessageList $messageList")
    }

    init {
        addView(messageListView)
        addView(connectionStateView)
        addView(messagePanel)

        messagePanel.sendButton.setOnClickListener {
            val text = messagePanel.inputView.text?.toString()
            if (text.isNullOrBlank()) return@setOnClickListener
            messagePanel.inputView.setText(StringUtils.EMPTY)
            controller.sendMessage(text)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingStart - paddingEnd
        val availableHeight = MeasureSpec.getSize(heightMeasureSpec) - paddingTop - paddingBottom - connectionSpacingVertical * 2
        val childWidthSpec = makeExactlySpec(availableWidth)
        connectionStateView.measure(makeUnspecifiedSpec(), makeUnspecifiedSpec())
        messagePanel.measure(childWidthSpec, makeUnspecifiedSpec())
        messageListView.measure(
            childWidthSpec,
            makeExactlySpec(availableHeight - connectionStateView.measuredHeight - messagePanel.measuredHeight)
        )
        setMeasuredDimension(
            measureDirection(widthMeasureSpec) { suggestedMinimumWidth },
            measureDirection(heightMeasureSpec) { suggestedMinimumHeight },
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        connectionStateView.layout(
            paddingStart + (measuredWidth - connectionStateView.measuredWidth).half,
            paddingTop + connectionSpacingVertical
        )
        messageListView.layout(paddingStart, connectionStateView.bottom + connectionSpacingVertical)
        messagePanel.layout(
            paddingStart,
            measuredHeight - paddingBottom - messagePanel.measuredHeight
        )
    }
}