package io.github.vladimirmi.internetradioplayer.data.repository

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.jakewharton.rxrelay2.BehaviorRelay
import io.github.vladimirmi.internetradioplayer.data.db.EqualizerDatabase
import io.github.vladimirmi.internetradioplayer.data.db.entity.EqualizerPresetEntity
import io.github.vladimirmi.internetradioplayer.data.utils.Preferences
import io.github.vladimirmi.internetradioplayer.domain.model.EqualizerConfig
import io.github.vladimirmi.internetradioplayer.domain.model.EqualizerPreset
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * Created by Vladimir Mikhalev 11.01.2019.
 */

class EqualizerRepository
@Inject constructor(db: EqualizerDatabase,
                    private val preferences: Preferences) {

    private val dao = db.equalizerDao()
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    val equalizerConfig: EqualizerConfig
    var presets: List<EqualizerPreset> = emptyList()
    private val currentPreset = BehaviorRelay.create<EqualizerPreset>()
    val currentPresetObs: Observable<EqualizerPreset> get() = currentPreset

    init {
        val tempEqualizer = Equalizer(-1, 1)
        equalizerConfig = EqualizerConfig.create(tempEqualizer)
        tempEqualizer.release()
    }

    fun createEqualizer(sessionId: Int) {
        equalizer = Equalizer(0, sessionId)
        equalizer?.enabled = true

        bassBoost = BassBoost(0, sessionId)
        bassBoost?.enabled = true

        virtualizer = Virtualizer(0, sessionId)
        virtualizer?.enabled = true

        currentPreset.value?.applyTo(equalizer, bassBoost, virtualizer)
    }

    fun releaseEqualizer() {
        equalizer?.release()
        equalizer = null
        bassBoost?.release()
        bassBoost = null
        virtualizer?.release()
        virtualizer = null
    }

    fun setBandLevel(band: Int, level: Int) {
        val preset = currentPreset.value?.let {
            val bands = it.bandLevels.toMutableList()
            bands[band] = level
            it.copy(bandLevels = bands)
        }
        updatePresetsWith(preset)
        equalizer?.setBandLevel(band.toShort(), level.toShort())
    }

    fun setBassBoostStrength(strength: Int) {
        updatePresetsWith(currentPreset.value?.copy(bassBoostStrength = strength))
        bassBoost?.setStrength(strength.toShort())
    }

    fun setVirtualizerStrength(strength: Int) {
        updatePresetsWith(currentPreset.value?.copy(virtualizerStrength = strength))
        virtualizer?.setStrength(strength.toShort())
    }

    fun saveCurrentPreset(): Completable {
        return Completable.fromCallable {
            currentPreset.value?.let { dao.insert(it.toEntity()) }
        }.subscribeOn(Schedulers.io())
    }

    fun setPreset(preset: EqualizerPreset) {
        preset.applyTo(equalizer, bassBoost, virtualizer)
        currentPreset.accept(preset)
    }

    fun selectPreset(index: Int) {
        val preset = presets[index]
        preset.applyTo(equalizer, bassBoost, virtualizer)
        preferences.globalPreset = preset.name
        currentPreset.accept(preset)
    }

    fun getSavedPresets(): Single<List<EqualizerPresetEntity>> {
        return dao.getPresets()
                .subscribeOn(Schedulers.io())
    }

    fun getGlobalPreset(): EqualizerPreset {
        return presets.find { it.name == preferences.globalPreset } ?: presets.first()
    }

    private fun updatePresetsWith(preset: EqualizerPreset?) {
        preset?.let {
            presets = presets.map { p -> if (p.name == preset.name) preset else p }
            currentPreset.accept(it)
        }
    }
}
