package com.chekurda.secret_pine.main_screen.presentation

import android.app.Activity
import android.os.Handler
import com.chekurda.common.base_fragment.BasePresenter
import com.chekurda.secret_pine.main_screen.presentation.views.device_picker.holder.DeviceViewHolder

/**
 * Контракт главного экрана.
 */
internal interface MainScreenContract {

    /**
     * View контракт главного экрана.
     */
    interface View {

        /**
         * Изменить состояние поиска девайсов.
         */
        fun updateSearchState(isRunning: Boolean)

        fun updateConnectionState(isConnected: Boolean)

        /**
         * Предоставить Activity.
         */
        fun provideActivity(): Activity

        fun provideHandler(): Handler
    }

    /**
     * Контракт презентера главного экрана.
     */
    interface Presenter : BasePresenter<View> {

        fun onPineModeSelected()

        fun onUserModeSelected()
    }
}