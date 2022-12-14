package com.chekurda.secret_pine.main_screen.contact

import androidx.fragment.app.Fragment
import com.chekurda.common.plugin_struct.Feature

/**
 * Фабрика фрагмента главного экрана.
 */
interface MainScreenFragmentFactory : Feature {

    fun createMainScreenFragment(): Fragment
}