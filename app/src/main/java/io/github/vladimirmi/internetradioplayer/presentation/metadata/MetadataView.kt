package io.github.vladimirmi.internetradioplayer.presentation.metadata

import com.arellomobile.mvp.MvpView
import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType

/**
 * Created by Vladimir Mikhalev 08.12.2017.
 */

interface MetadataView : MvpView {

    @StateStrategyType(AddToEndSingleStrategy::class)
    fun setMetadata(string: String)

    @StateStrategyType(AddToEndSingleStrategy::class)
    fun setMetadata(resId: Int)

    @StateStrategyType(AddToEndSingleStrategy::class)
    fun hideMetadata()

    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showMetadata()
}