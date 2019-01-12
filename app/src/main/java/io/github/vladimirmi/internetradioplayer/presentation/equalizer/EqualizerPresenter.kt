package io.github.vladimirmi.internetradioplayer.presentation.equalizer

import io.github.vladimirmi.internetradioplayer.domain.interactor.EqualizerInteractor
import io.github.vladimirmi.internetradioplayer.extensions.subscribeX
import io.github.vladimirmi.internetradioplayer.presentation.base.BasePresenter
import io.reactivex.rxkotlin.addTo
import javax.inject.Inject

/**
 * Created by Vladimir Mikhalev 09.01.2019.
 */

class EqualizerPresenter
@Inject constructor(private val equalizerInteractor: EqualizerInteractor) : BasePresenter<EqualizerView>() {

    override fun onFirstAttach(view: EqualizerView) {

        val bands = equalizerInteractor.bands
        val bandLevels = equalizerInteractor.bandLevels
        val range = equalizerInteractor.levelRange
        view.setBands(bands, bandLevels, range.first, range.second)
        view.setBassBoost(equalizerInteractor.bassBoost)
        view.setVirtualizer(equalizerInteractor.virtualizer)

        equalizerInteractor.equalizerInit.subscribeX()
                .addTo(dataSubs)
    }

    fun setBandLevel(band: Int, level: Int) {
        equalizerInteractor.setBandLevel(band, level)
    }

    fun setBassBoost(strength: Int) {
        equalizerInteractor.bassBoost = strength
    }

    fun setVirtualizer(strength: Int) {
        equalizerInteractor.virtualizer = strength
    }
}