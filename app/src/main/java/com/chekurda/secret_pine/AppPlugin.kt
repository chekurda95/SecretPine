package com.chekurda.secret_pine

import com.chekurda.common.plugin_struct.*
import com.chekurda.secret_pine.main_screen.contact.MainScreenFragmentFactory

object AppPlugin : BasePlugin<Unit>() {

    private lateinit var mainScreenFragmentFactory: FeatureProvider<MainScreenFragmentFactory>

    val gameFragmentFactory: MainScreenFragmentFactory by lazy {
        mainScreenFragmentFactory.get()
    }

    override val api: Set<FeatureWrapper<out Feature>> = setOf()
    override val customizationOptions: Unit = Unit

    override val dependency: Dependency = Dependency.Builder()
        .require(MainScreenFragmentFactory::class.java) { mainScreenFragmentFactory = it }
        .build()
}