package com.takusemba.sampleexoplayer

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaDrm
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory


class MainActivity : AppCompatActivity() {

  companion object {
    private const val USER_AGENT = "user-agent"
    private const val MP4_URL = "${BuildConfig.END_POINT}/sample.mp4"
    private const val HLS_URL = "${BuildConfig.END_POINT}/nondrm/hls/h264_720p.m3u8"
    private const val DASH_URL = "${BuildConfig.END_POINT}/not/exist"
    private const val DRM_DASH_URL = "${BuildConfig.END_POINT}/drm/dash/sample.mpd"
    private const val DRM_HLS_URL = "${BuildConfig.END_POINT}/drm/hls/h264_720p.m3u8"
    private const val DRM_LICENSE_URL = "https://widevine-proxy.appspot.com/proxy"
  }

  enum class StreamingType {
    DASH, HLS
  }

  private val handler = Handler()
  private val bandwidthMeter = DefaultBandwidthMeter()
  private val drmCallback = HttpMediaDrmCallback(DRM_LICENSE_URL,
      DefaultHttpDataSourceFactory(USER_AGENT)
  )
  private val drmSessionManager = DefaultDrmSessionManager(C.WIDEVINE_UUID,
      FrameworkMediaDrm.newInstance(C.WIDEVINE_UUID), drmCallback, null, handler, null)
  private val selector = DefaultTrackSelector()
  private val loadControl = DefaultLoadControl()
  private val dataSourceFactory = DefaultDataSourceFactory(this, bandwidthMeter,
      DefaultHttpDataSourceFactory(USER_AGENT, bandwidthMeter)
  )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val player = initPlayer(isDrm = true, type = StreamingType.HLS)
    player.playWhenReady = true
  }

  private fun initPlayer(isDrm: Boolean, type: StreamingType): SimpleExoPlayer {
    val renderersFactory = if (isDrm)
      DefaultRenderersFactory(this, drmSessionManager) else DefaultRenderersFactory(this)
    val player = ExoPlayerFactory.newSimpleInstance(renderersFactory, selector, loadControl)

    val playerView = findViewById<PlayerView>(R.id.player_view)
    playerView.player = player

    val mediaSource = when (type) {
      StreamingType.DASH -> createDashSource(if (isDrm) DRM_DASH_URL else DASH_URL)
      StreamingType.HLS -> createHlsSource(if (isDrm) DRM_HLS_URL else HLS_URL)
    }

    player.prepare(mediaSource)

    return player
  }

  private fun createDashSource(url: String): DashMediaSource {
    return DashMediaSource.Factory(
        DefaultDashChunkSource.Factory(dataSourceFactory),
        dataSourceFactory
    ).createMediaSource(Uri.parse(url))
  }

  private fun createHlsSource(url: String): HlsMediaSource {
    return HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(url))
  }
}
