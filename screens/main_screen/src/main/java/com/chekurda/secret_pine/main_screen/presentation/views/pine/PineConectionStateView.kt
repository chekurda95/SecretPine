package com.chekurda.secret_pine.main_screen.presentation.views.pine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.chekurda.common.half
import com.chekurda.design.custom_view_tools.TextLayout
import com.chekurda.design.custom_view_tools.utils.MeasureSpecUtils
import com.chekurda.design.custom_view_tools.utils.dp
import com.chekurda.design.custom_view_tools.utils.safeRequestLayout
import com.chekurda.secret_pine.main_screen.R
import com.chekurda.secret_pine.main_screen.presentation.views.drawables.AnimatedDotsDrawable

internal class PineConnectionStateView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class State(val text: String, @DrawableRes val backgroundRes: Int) {
        PREPARING("Pine preparing".uppercase(), R.drawable.pine_search_state_background),
        SEARCH_PINE_LOVERS("Pine lovers searching".uppercase(), R.drawable.pine_search_state_background),
        CONNECTED("Connected".uppercase(), R.drawable.pine_connected_background)
    }

    private val textLayout = TextLayout {
        paint.apply {
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            textSize = dp(17).toFloat()
        }
    }

    private val dotsDrawable = AnimatedDotsDrawable().apply {
        callback = this@PineConnectionStateView
        params = AnimatedDotsDrawable.DotsParams(size = dp(3))
        textColor = Color.WHITE
    }
    private val dotsSpacing = dp(2)

    var state: State = State.PREPARING
        set(value) {
            field = value
            val isChanged = textLayout.configure { text = value.text }
            if (isChanged) {
                background = ContextCompat.getDrawable(context, value.backgroundRes)
                safeRequestLayout()
            }
        }

    init {
        state = State.PREPARING
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        dotsDrawable.setVisible(state != State.CONNECTED, false)
        setMeasuredDimension(
            MeasureSpecUtils.measureDirection(widthMeasureSpec) { suggestedMinimumWidth },
            MeasureSpecUtils.measureDirection(heightMeasureSpec) { suggestedMinimumHeight },
        )
    }

    override fun getSuggestedMinimumWidth(): Int =
        textLayout.width + paddingStart + paddingEnd + if (dotsDrawable.isVisible) dotsSpacing + dotsDrawable.intrinsicWidth else 0

    override fun getSuggestedMinimumHeight(): Int =
        textLayout.height + paddingTop + paddingBottom

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (dotsDrawable.isVisible) {
            textLayout.layout(
                paddingStart + (measuredWidth - paddingStart - paddingEnd - textLayout.width - dotsDrawable.intrinsicWidth).half,
                paddingTop + (measuredHeight - paddingTop - paddingBottom - textLayout.height).half
            )
            val dotsLeft = textLayout.right + dotsSpacing
            val dotsTop = textLayout.top + textLayout.baseline - dotsDrawable.intrinsicHeight
            dotsDrawable.setBounds(
                dotsLeft,
                dotsTop,
                dotsLeft + dotsDrawable.intrinsicWidth,
                dotsTop + dotsDrawable.intrinsicHeight
            )
        } else {
            textLayout.layout(
                paddingStart + (measuredWidth - paddingStart - paddingEnd - textLayout.width).half,
                paddingTop + (measuredHeight - paddingTop - paddingBottom - textLayout.height).half
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        textLayout.draw(canvas)
        dotsDrawable.draw(canvas)
    }

    override fun verifyDrawable(who: Drawable): Boolean =
        who == dotsDrawable || super.verifyDrawable(who)
}