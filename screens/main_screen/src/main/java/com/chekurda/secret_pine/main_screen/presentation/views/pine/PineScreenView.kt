package com.chekurda.secret_pine.main_screen.presentation.views.pine

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.*
import android.widget.FrameLayout
import com.chekurda.design.custom_view_tools.utils.dp

internal class PineScreenView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val stateView = PineConnectionStateView(context)

    var state: PineConnectionStateView.State
        get() = stateView.state
        set(value) {
            stateView.state = value
        }

    init {
        val stateViewLp = LayoutParams(LayoutParams.MATCH_PARENT, WRAP_CONTENT).apply {
            marginStart = dp(30)
            marginEnd = dp(30)
            gravity = Gravity.CENTER
        }
        addView(stateView, stateViewLp)
    }
}