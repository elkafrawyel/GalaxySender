package com.castgalaxy.app.ui.player

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.castgalaxy.app.R
import com.castgalaxy.app.entity.Video
import com.castgalaxy.app.ui.expandedcontrols.ExpandedControlsActivity
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ts.DefaultTsPayloadReaderFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadOptions
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.*
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage
import kotlinx.android.synthetic.main.activity_player.*

class PlayerActivity : AppCompatActivity(), VideoListener, Player.EventListener {

    private val TAG = "GalaxyCast"
    private var mCastContext: CastContext? = null
    private var mediaRouteMenuItem: MenuItem? = null
    private var mIntroductoryOverlay: IntroductoryOverlay? = null
    private var mCastStateListener: CastStateListener? = null
    private var mLocation: PlaybackLocation? = null
    private var mCastSession: CastSession? = null
    private var mSessionManagerListener: SessionManagerListener<CastSession>? = null
    private var videoId: String = ""
    private var videoUrl: String? = null
    private var video: Video? = null
    //       "https://commondatastorage.googleapis.com/gtv-videos-bucket/CastVideos/mp4/Sintel.mp4"
    //"https://r1---sn-4g5e6nle.googlevideo.com/videoplayback?initcwndbps=133750&source=youtube&dur=300.081&clen=4551499&txp=5511222&c=WEB&lmt=1544652033664836&ipbits=0&gir=yes&mn=sn-4g5e6nle%2Csn-h0jeen7d&ip=2a03%3Ab0c0%3A3%3Ad0%3A%3A192%3A3001&mm=31%2C26&fvip=1&ms=au%2Conr&mv=m&mt=1554215724&sparams=clen%2Cdur%2Cei%2Cgir%2Cid%2Cinitcwndbps%2Cip%2Cipbits%2Citag%2Ckeepalive%2Clmt%2Cmime%2Cmm%2Cmn%2Cms%2Cmv%2Cpl%2Crequiressl%2Csource%2Cexpire&pl=53&id=o-AA357vjqvS-y5vNP4y69nMnEmWofdMM_9aTLYZKqvH1T&mime=audio%2Fwebm&ei=eHOjXOOWGIPJ1wK4jLaQDQ&itag=251&keepalive=yes&requiressl=yes&signature=7144AE89FDAC3643A43E616BB07304EF0816E4A3.788EDC12F43377F21B5054858D8D8D8A1DB013C3&expire=1554237400&key=yt6&ratebypass=yes"
    private var player: SimpleExoPlayer? = null
    private var audioManager: AudioManager? = null
    private val bandwidthMeter = DefaultBandwidthMeter()

    private val viewModel: PlayerViewModel by lazy { ViewModelProviders.of(this).get(PlayerViewModel::class.java) }

    /**
     * indicates whether we are doing a local or a remote playback
     */
    enum class PlaybackLocation {
        LOCAL,
        REMOTE
    }

    companion object {
        fun start(context: Context, videoId: String) {
            val intent = Intent(context, PlayerActivity::class.java)
            intent.putExtra("videoId", videoId)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        setupCastListener()
        setupActionBar()

        mCastStateListener = CastStateListener { newState ->
            if (newState != CastState.NO_DEVICES_AVAILABLE) {
                showIntroductoryOverlay()
            }
        }

        mCastContext = CastContext.getSharedInstance(this);
        mCastSession = mCastContext!!.sessionManager.currentCastSession

        if (mCastSession != null) {
            if (mCastSession?.isConnected!!) {
                if (player != null) {
                    updatePlaybackLocation(PlaybackLocation.REMOTE)
                    castVideo(player?.currentPosition!!)
                } else {
                    updatePlaybackLocation(PlaybackLocation.REMOTE)
                    castVideo(0L)
                }
            }
        } else {
            updatePlaybackLocation(PlaybackLocation.LOCAL)
            if (videoUrl != null)
                playVideo(videoUrl!!)
        }

        viewModel.videoLiveData.observe(this, Observer { onVideoResponse(it) })

        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        videoId = intent.getStringExtra("videoId")
        viewModel.getVideo(videoId)

        retry.setOnClickListener { Retry() }

    }

    private fun Retry() {
        viewModel.getVideo(videoId)
    }

    private fun onVideoResponse(resource: PlayerViewModel.VideoState?) {
        when (resource) {

            PlayerViewModel.VideoState.Loading -> onVideoInfoLoading()
            is PlayerViewModel.VideoState.Success -> onVideoInfoSuccess(resource.data)
            is PlayerViewModel.VideoState.Error -> onVideoInfoError(resource.message)
            is PlayerViewModel.VideoState.NoConnection -> onVideoInfoNoConnection()
            null -> {
            }
        }
    }

    private fun onVideoInfoLoading() {
        retry.visibility = View.GONE
        loadingView.visibility = View.VISIBLE
    }

    private fun onVideoInfoNoConnection() {
        Toast.makeText(
            this@PlayerActivity,
            "No Internet Connection",
            Toast.LENGTH_LONG
        ).show()
        loadingView.visibility = View.GONE
        retry.visibility = View.VISIBLE
    }

    private fun onVideoInfoError(message: String) {
        Toast.makeText(
            this@PlayerActivity, message,
            Toast.LENGTH_LONG
        ).show()
        loadingView.visibility = View.GONE
        retry.visibility = View.VISIBLE
    }

    private fun onVideoInfoSuccess(data: Video) {
        retry.visibility = View.GONE
        loadingView.visibility = View.GONE
        video = data
        if (video != null && video!!.streams.isNotEmpty()) {
            videoUrl = video?.streams!![0].url!!
            setupActionBar()
            startCast()
        } else {
            Toast.makeText(
                this@PlayerActivity,
                "Failed to get video info.",
                Toast.LENGTH_LONG
            ).show()
            loadingView.visibility = View.GONE
            retry.visibility = View.VISIBLE
        }
    }

    private fun playVideo(mUrl: String) {
        if (player != null) {
            releasePlayer()
        }
        val adaptiveTrackSelectionFactory = AdaptiveTrackSelection.Factory()
        player = ExoPlayerFactory.newSimpleInstance(
            this, DefaultRenderersFactory(
                this,
                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            ),
            DefaultTrackSelector(adaptiveTrackSelectionFactory)
        )
        playerView.player = player
        val videoUri = Uri.parse(mUrl)
        val mediaSource = buildMediaSource(videoUri)
        player!!.prepare(mediaSource)
        player!!.playWhenReady = true
        playerView.useController = true

        player!!.addListener(this)
        player!!.addVideoListener(this)

    }


    override fun onStart() {
        super.onStart()
        hideSystemUI()

        if (player != null) {
            releasePlayer()
        }

    }


    private fun buildMediaSource(uri: Uri): MediaSource {
        val defaultExtractorsFactory = DefaultExtractorsFactory()
        defaultExtractorsFactory.setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS)
        defaultExtractorsFactory.setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES)

        return ExtractorMediaSource.Factory(DataSource.Factory {
            DefaultDataSourceFactory(
                this@PlayerActivity,
                bandwidthMeter,
                DefaultHttpDataSourceFactory(
                    Util.getUserAgent(this@PlayerActivity, getString(R.string.app_name)),
                    bandwidthMeter
                )
            ).createDataSource()
        }).setExtractorsFactory(defaultExtractorsFactory)
            .createMediaSource(uri)
    }

    private fun releasePlayer() {
        if (player != null) {
            player!!.removeListener(this)
            player!!.removeVideoListener(this)
            player!!.release()
            player = null
        }

    }

    private fun setupCastListener() {
        mSessionManagerListener = object : SessionManagerListener<CastSession> {

            override fun onSessionEnded(session: CastSession, error: Int) {
                Log.i(TAG, "Ended")
            }

            override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
                onApplicationConnected(session)
                Log.i(TAG, "Resumed")
            }

            override fun onSessionResumeFailed(session: CastSession, error: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionStarted(session: CastSession, sessionId: String) {
                onApplicationConnected(session)
                Log.i(TAG, "Started")
            }

            override fun onSessionStartFailed(session: CastSession, error: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionResuming(session: CastSession, sessionId: String) {}

            override fun onSessionSuspended(session: CastSession, reason: Int) {
                onApplicationDisconnected()
            }

            override fun onSessionStarting(session: CastSession) {
                loadingView.visibility = View.VISIBLE
                Log.i(TAG, "Starting")
            }

            override fun onSessionEnding(session: CastSession) {
                onApplicationDisconnected()
                Log.i(TAG, "Ending")
            }

            private fun onApplicationConnected(castSession: CastSession) {
                mCastSession = castSession
                startCast()
                return
            }

            private fun onApplicationDisconnected() {
                stopCast()
            }

        }
    }

    private fun startCast() {
        if (mCastSession != null) {
            if (mCastSession?.isConnected!!) {
                if (player != null) {
                    updatePlaybackLocation(PlaybackLocation.REMOTE)
                    castVideo(player?.currentPosition!!)
                } else {
                    updatePlaybackLocation(PlaybackLocation.REMOTE)
                    castVideo(0L)
                }
                player?.playWhenReady = false
                updatePlaybackLocation(PlaybackLocation.REMOTE)
                invalidateOptionsMenu()
                loadingView.visibility = View.GONE
            }
        } else {
            updatePlaybackLocation(PlaybackLocation.LOCAL)
            if (videoUrl != null)
                playVideo(videoUrl!!)
        }

        if (player != null) {
            castVideo(player?.currentPosition!!)
        } else {
            castVideo(0L)
        }

    }

    private fun stopCast() {
        updatePlaybackLocation(PlaybackLocation.LOCAL)
        mLocation = PlaybackLocation.LOCAL
        val position = mCastSession?.remoteMediaClient?.mediaStatus?.streamPosition
        if (player == null) {
            playVideo(videoUrl!!)
        }
        player?.seekTo(position!!)
        player?.playWhenReady = true
        Log.i(TAG, "Position : $position")

        invalidateOptionsMenu()
        loadingView.visibility = View.GONE
    }

    private fun castVideo(position: Long) {
        if (video != null) {
            loadRemoteMedia(position, true)
        }
    }

    private fun loadRemoteMedia(position: Long, autoPlay: Boolean) {
        if (mCastSession == null) {
            return
        }
        val remoteMediaClient = mCastSession!!.remoteMediaClient ?: return
        remoteMediaClient.registerCallback(object : RemoteMediaClient.Callback() {
            override fun onStatusUpdated() {
                val intent = Intent(this@PlayerActivity, ExpandedControlsActivity::class.java)
                startActivity(intent)
                remoteMediaClient.unregisterCallback(this)
            }
        })
        remoteMediaClient.load(
            buildMediaInfo(),
            MediaLoadOptions.Builder()
                .setAutoplay(autoPlay)
                .setPlayPosition(position).build()
        )
    }

    private fun buildMediaInfo(): MediaInfo {
        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)

        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, video?.uploader)
        movieMetadata.putString(MediaMetadata.KEY_TITLE, video?.title)
        movieMetadata.addImage(WebImage(Uri.parse(video?.thumbnail)))

        return MediaInfo.Builder(videoUrl)
            .setMetadata(movieMetadata)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("videos/mp4")
            .build()
    }

    private fun updatePlaybackLocation(location: PlaybackLocation) {
        mLocation = location
        if (location == PlaybackLocation.LOCAL) {
            if (player != null) {
                player?.playWhenReady = true
            }
        } else {
            if (player != null) {
                player?.playWhenReady = false
            }
        }
    }

    private fun setupActionBar() {
        if (video != null) {
            toolbar.title = video!!.title
        } else {
            toolbar.title = resources.getString(R.string.app_name)
        }
        setSupportActionBar(toolbar)
    }

    override fun onResume() {
        Log.d(TAG, "onResume() was called")
        if (mCastContext != null) {
            mCastContext!!.sessionManager.addSessionManagerListener(
                mSessionManagerListener, CastSession::class.java
            )
            if (mCastSession != null && mCastSession!!.isConnected) {
                updatePlaybackLocation(PlaybackLocation.REMOTE)
            } else {
                if (player == null && videoUrl != null) {
                    playVideo(videoUrl!!)
                }
                updatePlaybackLocation(PlaybackLocation.LOCAL)
            }
        }
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() was called")
        if (mCastContext != null) {
            mCastContext!!.removeCastStateListener(mCastStateListener)
            mCastContext!!.sessionManager.removeSessionManagerListener(
                mSessionManagerListener, CastSession::class.java
            )
        }
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy is called")
        super.onDestroy()
    }

    private fun showIntroductoryOverlay() {
        if (mIntroductoryOverlay != null) {
            mIntroductoryOverlay!!.remove()
        }
        if (mediaRouteMenuItem != null && mediaRouteMenuItem!!.isVisible()) {
            Handler().post {
                mIntroductoryOverlay = IntroductoryOverlay.Builder(
                    this, mediaRouteMenuItem
                )
                    .setTitleText("Introducing Cast")
//                    .setSingleTime()
                    .setOnOverlayDismissedListener { mIntroductoryOverlay = null }
                    .build()
                mIntroductoryOverlay!!.show()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu);
        menuInflater.inflate(R.menu.player_menu, menu);
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(
            applicationContext, menu,
            R.id.player_cast_menu_item
        );
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.player_quality) {
            if (video == null) {
                Toast.makeText(this, "No Video Info Found.", Toast.LENGTH_LONG).show()
            } else {
                showQualityDialog()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showQualityDialog() {
        // Late initialize an alert dialog object
        lateinit var dialog: AlertDialog

        // Initialize an array of colors
        var streamQualities = emptyArray<String>()

        streamQualities = video!!.streams.mapNotNull {
            it.format+" - " + it.extension
        }.toTypedArray()

        // Initialize a new instance of alert dialog builder object
        val builder = AlertDialog.Builder(this, R.style.MyDialogTheme)

        // Set a title for alert dialog
        builder.setTitle("Choose a quality.")

        // Set the single choice items for alert dialog with initial selection
        builder.setSingleChoiceItems(streamQualities, -1) { _, which ->
            // Get the dialog selected item
            val streamUrl = video!!.streams[which].url

            // Change the layout background color using user selection
            videoUrl = streamUrl
            if (mCastSession != null && mCastSession!!.isConnected) {
                startCast()
                updatePlaybackLocation(PlaybackLocation.REMOTE)
            } else {
                val lastPosition = player?.currentPosition
                playVideo(videoUrl!!)
                player?.seekTo(lastPosition!!)

                updatePlaybackLocation(PlaybackLocation.LOCAL)
            }
            // Dismiss the dialog
            dialog.dismiss()
        }


        // Initialize the AlertDialog using builder object
        dialog = builder.create()

        // Finally, display the alert dialog
        dialog.show()
    }

    override fun onTimelineChanged(timeline: Timeline, manifest: Any?, reason: Int) {

    }

    override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {

    }

    override fun onLoadingChanged(isLoading: Boolean) {
//        if (isLoading) loadingView.visibility = View.VISIBLE else loadingView.visibility = View.GONE
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

    }

    override fun onRepeatModeChanged(repeatMode: Int) {

    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {

    }

    override fun onPlayerError(error: ExoPlaybackException) {
        Toast.makeText(
            this@PlayerActivity,
            "Error Happened ${error.message}",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onPositionDiscontinuity(reason: Int) {

    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {

    }

    override fun onSeekProcessed() {

    }

    override fun onVideoSizeChanged(
        width: Int, height: Int, unappliedRotationDegrees: Int,
        pixelWidthHeightRatio: Float
    ) {
    }

    override fun onRenderedFirstFrame() {

    }
}