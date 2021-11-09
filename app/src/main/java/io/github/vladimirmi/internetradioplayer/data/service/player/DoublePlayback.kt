package io.github.vladimirmi.internetradioplayer.data.service.player

import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY
import android.net.Uri
import android.net.wifi.WifiManager
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.util.Util
import io.github.vladimirmi.internetradioplayer.BuildConfig
import io.github.vladimirmi.internetradioplayer.R
import io.github.vladimirmi.internetradioplayer.data.preference.AudioFocusPreference
import io.github.vladimirmi.internetradioplayer.data.preference.Preferences
import io.github.vladimirmi.internetradioplayer.data.service.PlayerCallback
import io.github.vladimirmi.internetradioplayer.data.service.PlayerService
import io.github.vladimirmi.internetradioplayer.di.Scopes
import io.github.vladimirmi.internetradioplayer.extensions.runOnUiThread
import io.github.vladimirmi.internetradioplayer.extensions.subscribeX
import io.github.vladimirmi.internetradioplayer.extensions.wifiManager
import okhttp3.OkHttpClient


const val DOUBLE_STOP_DELAY = 60000L // default stop delay 1 min
private const val STOP_DELAY_HEADSET = 3 * 60000L // stop delay on headset unplug

class DoublePlayback(private val service: PlayerService,
                     private val playerCallback1: PlayerCallback,
                     private val playerCallback2: PlayerCallback,
                     private val preferences: Preferences) {

    private var playAgainOnHeadset = false
    private var player1: SimpleExoPlayer? = null
    private var player2: SimpleExoPlayer? = null

    private val httpClient = Scopes.app.getInstance(OkHttpClient::class.java)
    private val loadControl = Scopes.app.getInstance(LoadControl::class.java)

    private val wifiLock = service.wifiManager
            .createWifiLock(WifiManager.WIFI_MODE_FULL, BuildConfig.APPLICATION_ID)

    private val analyticsListener = object : AnalyticsListener {

        override fun onAudioSessionId(eventTime: AnalyticsListener.EventTime?, audioSessionId: Int) {
            playerCallback1.setStartAudioSessionId(audioSessionId)
        }
    }

    init {
        preferences.observe<AudioFocusPreference>(Preferences.KEY_AUDIO_FOCUS)
                .subscribeX { setupAudioAttributes(it) }
    }

    fun prepare(uri: Uri) {
        runOnUiThread {
            if (player1 == null) createPlayer1()
            if (player2 == null) createPlayer2()
            if (Util.isLocalFileUri(uri)) {
                prepareFilePlayer(uri)
                registerAudioNoisyReceiver()
            } else {
                prepareHttpPlayer(uri)
                holdResources()
            }
        }
    }

    fun play() {
        runOnUiThread {
            player1?.volume = 0f
            player1?.playWhenReady = true
        }
    }

    fun pause() {
        runOnUiThread {
            player1?.playWhenReady = false
        }
    }

    fun stop() {
        runOnUiThread {
            playAgainOnHeadset = false
            player1?.stop()
            releaseResources()
        }
    }

    fun seekTo(position: Long) {
        runOnUiThread {
            player1?.seekTo(position)
        }
    }

    fun releasePlayer() {
        runOnUiThread {
            player1?.removeListener(playerCallback1)
            playerCallback1.player = null
            player1?.removeAnalyticsListener(analyticsListener)
            player1?.release()
            player1 = null

            player1?.removeListener(playerCallback1)
            playerCallback1.player = null
            player1?.removeAnalyticsListener(analyticsListener)
            player1?.release()
            player1 = null
        }
    }


    private fun createPlayer1() {
        player1 = ExoPlayerFactory.newSimpleInstance(service, AudioRenderersFactory(service),
                DefaultTrackSelector(), loadControl)
        player1?.addListener(playerCallback1)
        playerCallback1.player = player1
        player1?.addAnalyticsListener(analyticsListener)
        setupAudioAttributes(preferences.audioFocus)
    }
    private fun createPlayer2() {
        player2 = ExoPlayerFactory.newSimpleInstance(service, AudioRenderersFactory(service),
                DefaultTrackSelector(), loadControl)
        player2?.addListener(playerCallback2)
        playerCallback1.player = player2
        player2?.addAnalyticsListener(analyticsListener)
        setupAudioAttributes(preferences.audioFocus)
    }

    private fun setupAudioAttributes(preference: AudioFocusPreference) {
        player1 ?: return
        val builder = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
        when (preference) {
            AudioFocusPreference.Duck -> builder.setContentType(C.CONTENT_TYPE_MUSIC)
            AudioFocusPreference.Pause -> builder.setContentType(C.CONTENT_TYPE_SPEECH)
        }
        player1?.setAudioAttributes(builder.build(), true)
    }

    private fun prepareHttpPlayer(uri: Uri) {
        val userAgent = Util.getUserAgent(service, service.getString(R.string.app_name))
        val mediaSource = ExtractorMediaSource.Factory {
            IcyHttpDataSource(httpClient, userAgent, playerCallback1)
        }
                .setLoadErrorHandlingPolicy(ErrorHandlingPolicy())
                .createMediaSource(uri)

        player1?.prepare(mediaSource)
    }

    private fun prepareFilePlayer(uri: Uri) {
        val mediaSource = ExtractorMediaSource.Factory(::FileDataSource)
                .setExtractorsFactory(DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true))
                .createMediaSource(uri)
        player1?.prepare(mediaSource)
    }

    private fun holdResources() {
        registerAudioNoisyReceiver()
        if (!wifiLock.isHeld) wifiLock.acquire()
    }

    private fun releaseResources() {
        unregisterAudioNoisyReceiver()
        if (wifiLock.isHeld) wifiLock.release()
    }

    private val audioNoisyReceiver = object : BroadcastReceiver() {
        val filter = IntentFilter(ACTION_AUDIO_BECOMING_NOISY).apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        }

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when {
                action == ACTION_AUDIO_BECOMING_NOISY -> {
                    playAgainOnHeadset = player1?.playWhenReady ?: false
                    service.onPauseCommand(stopDelay = STOP_DELAY_HEADSET)

                }
                playAgainOnHeadset && action == Intent.ACTION_HEADSET_PLUG
                        && intent.getIntExtra("state", 0) == 1
                        ||
                        playAgainOnHeadset && action == BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED
                        && intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, 0) == BluetoothHeadset.STATE_CONNECTED
                -> service.onPlayCommand()
            }
        }
    }

    @Volatile private var audioNoisyReceiverRegistered = false

    private fun registerAudioNoisyReceiver() {
        if (!audioNoisyReceiverRegistered) {
            service.registerReceiver(audioNoisyReceiver, audioNoisyReceiver.filter)
            audioNoisyReceiverRegistered = true
        }
    }

    private fun unregisterAudioNoisyReceiver() {
        if (audioNoisyReceiverRegistered) {
            service.unregisterReceiver(audioNoisyReceiver)
            audioNoisyReceiverRegistered = false
        }
    }
}
