package io.github.vladimirmi.internetradioplayer.presentation.getstarted

import com.arellomobile.mvp.MvpView
import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.OneExecutionStateStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import io.github.vladimirmi.internetradioplayer.presentation.base.ToolbarBuilder

/**
 * Created by Vladimir Mikhalev 23.12.2017.
 */

interface GetStartedView : MvpView {

    @StateStrategyType(AddToEndSingleStrategy::class)
    fun buildToolbar(builder: ToolbarBuilder)

    @StateStrategyType(OneExecutionStateStrategy::class)
    fun openAddStationDialog()
}
