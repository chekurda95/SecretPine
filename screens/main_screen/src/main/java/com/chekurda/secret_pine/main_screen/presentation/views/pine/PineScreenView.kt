package com.chekurda.secret_pine.main_screen.presentation.views.pine

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.*
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import com.chekurda.design.custom_view_tools.utils.dp
import com.chekurda.secret_pine.main_screen.R
import com.chekurda.secret_pine.main_screen.presentation.views.ConnectionStateView

internal class PineScreenView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val stateView = ConnectionStateView(context).apply {
        state = ConnectionStateView.State.PREPARING
        updatePadding(top = dp(40), bottom = dp(40))
    }

    var state: ConnectionStateView.State
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
        background = ContextCompat.getDrawable(context, R.drawable.main_screen_background)
    }
}