package com.castgalaxy.app.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import at.huber.youtubeExtractor.Format
import at.huber.youtubeExtractor.VideoMeta
import at.huber.youtubeExtractor.YouTubeExtractor
import at.huber.youtubeExtractor.YtFile
import com.castgalaxy.app.GalaxyCastApplication
import com.castgalaxy.app.R
import com.castgalaxy.app.entity.Video
import com.castgalaxy.app.entity.YoutubeVideoInfo
import com.castgalaxy.app.ui.ControlsActivity
import com.crashlytics.android.Crashlytics
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
import com.google.firebase.database.*
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_player.*

private const val VIDEO = "videoId"
private const val URL = "url"
const val CONNECTED = "connected"


class PlayerActivity : AppCompatActivity(), VideoListener, Player.EventListener {

    private val TAG = "GalaxyCast"
    private var videoId: String? = null
    private var url: String? = null
    private var videoUrl: String? = null
    private var video: Video? = null
    private var player: SimpleExoPlayer? = null
    private var audioManager: AudioManager? = null
    private val bandwidthMeter = DefaultBandwidthMeter()
    private var youtubeLinksArray = arrayListOf<YoutubeVideoInfo>()

    private val BASE_URL = "https://www.youtube.com"

    private val viewModel: PlayerViewModel by lazy { ViewModelProviders.of(this).get(PlayerViewModel::class.java) }

    enum class VideoType {
        VIDEO,
        URL
    }

    companion object {
        fun start(context: Context, videoId: String?, url: String?) {
            val intent = Intent(context, PlayerActivity::class.java)
            if (videoId != null) {
                intent.putExtra(VIDEO, videoId)
            } else {
                intent.putExtra(URL, url)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)
        setupActionBar()

        viewModel.videoLiveData.observe(this, Observer { onVideoResponse(it) })

        audioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        videoId = intent.getStringExtra(VIDEO)
        url = intent.getStringExtra(URL)

        when {
            videoId != null -> {
                extractYoutubeUrl(getYoutubeLink(videoId!!))
            }
            url != null -> viewModel.getVideo(url!!, VideoType.URL)
            else -> {
            }
        }

        retry.setOnClickListener { retry() }

        Fabric.with(this@PlayerActivity, Crashlytics())

    }

    private fun getYoutubeLink(videoId: String): String {
        return "$BASE_URL/watch?v=$videoId"

    }

    private fun extractYoutubeUrl(mYoutubeLink: String) {
        @SuppressLint("StaticFieldLeak") val mExtractor = object : YouTubeExtractor(this) {
            override fun onExtractionComplete(sparseArray: SparseArray<YtFile>?, videoMeta: VideoMeta) {
                if (sparseArray != null) {
                    // Initialize an array of colors


                    val size = sparseArray.size()
                    for (i in 0 until size - 1) {
                        val item = sparseArray.valueAt(i) as YtFile
                        val format = item.format as Format
                        val ext = format.ext
                        val height = format.height.toString()
                        val url = item.url
                        val info = YoutubeVideoInfo("$ext / $height", url)
                        if (ext.equals("mp4")) {
                            youtubeLinksArray.add(info)
                        }
                    }

                    playVideo(mUrl = youtubeLinksArray[0].url)
                }
            }
        }
        mExtractor.extract(mYoutubeLink, true, true)
    }

    private fun retry() {
        if (videoId != null) {
            viewModel.getVideo(videoId!!, VideoType.VIDEO)
        } else if (url != null) {
            viewModel.getVideo(url!!, VideoType.URL)
        } else {

        }

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
    }

    private fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun onVideoInfoError(message: String) {
        Toast.makeText(
            this@PlayerActivity, message,
            Toast.LENGTH_LONG
        ).show()
        loadingView.visibility = View.GONE
    }

    private fun onVideoInfoSuccess(data: Video) {
        retry.visibility = View.GONE
        loadingView.visibility = View.GONE
        video = data
        showQualityDialog()
    }

    private fun playVideo(mUrl: String) {
        videoUrl = mUrl
        if (player != null) {
            releasePlayer()
        }

        val code = GalaxyCastApplication.getPreferenceHelper().code
        if (code != null) {

            tvRef = FirebaseDatabase.getInstance().reference.child("TVs").child(code)
            //if screen connected or not

            valueEventListener = object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val connected = dataSnapshot.child(CONNECTED).value
                        if (connected == "1") {
                            //change menu
                            openControlsActivity(code)
                            tvRef.removeEventListener(this)
                            finish()
                        }
                    }
                }
            }
            tvRef.addValueEventListener(valueEventListener!!)
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
        Log.i("MyApp", mUrl)
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
        if (player == null && videoUrl != null) {
            playVideo(videoUrl!!)
        }
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() was called")
        releasePlayer()
    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy is called")
        releasePlayer()
        super.onDestroy()
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

    var isConnected = false
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu);
        menuInflater.inflate(R.menu.player_menu, menu);

        val code = GalaxyCastApplication.getPreferenceHelper().code
        if (code != null) {

            tvRef = FirebaseDatabase.getInstance().reference.child("TVs").child(code)
            //if screen connected or not

            valueEventListener = object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val connected = dataSnapshot.child(CONNECTED).value
                        if (connected == "1") {
                            //change menu
                            menu?.findItem(R.id.player_connect)?.icon =
                                resources.getDrawable(R.drawable.ic_cast_connected_white_24dp)
                            isConnected = true


                        } else {
                            menu?.findItem(R.id.player_connect)?.icon =
                                resources.getDrawable(R.drawable.ic_cast_white_24dp)
                            isConnected = false
                        }
                    } else {
                        menu?.findItem(R.id.player_connect)?.icon =
                            resources.getDrawable(R.drawable.ic_cast_white_24dp)
                        isConnected = false
                    }
                }
            }
            tvRef.addValueEventListener(valueEventListener!!)
        } else {
            menu?.findItem(R.id.player_connect)?.icon =
                resources.getDrawable(R.drawable.ic_cast_white_24dp)
            isConnected = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.player_quality) {
            if (videoId != null) {
                showQualityDialogYoutube()
            } else {
                showQualityDialog()
            }
        } else if (item?.itemId == R.id.player_connect) {
            val code = GalaxyCastApplication.getPreferenceHelper().code

            if (isConnected) {
                val dialog = AlertDialog.Builder(this@PlayerActivity)
                    .setTitle("Disconnect ??")
                    .setIcon(R.drawable.logo)
                    .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()

                dialog.setOnShowListener { dialog1 ->

                    val OkBtn = (dialog1 as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                    OkBtn.setOnClickListener {
                        tvRef.child(CONNECTED).setValue("0")
                        toast("Disconnected from $code")

                        dialog1.cancel()
                    }

                    val cancel = dialog1.getButton(AlertDialog.BUTTON_NEGATIVE)
                    cancel.setOnClickListener {
                        dialog1.cancel()
                    }
                }

                dialog.show()
            } else {
                if (code != null) {
                    connectToTvDevice(code)
                } else {
                    openGetConnectionCode()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showQualityDialog() {
        // Late initialize an alert dialog object
        lateinit var dialog: AlertDialog

        // Initialize an array of colors
        var streamQualities = emptyArray<String>()

        streamQualities = video!!.streams.map {
            it.format + " - " + it.extension
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
            val lastPosition = player?.currentPosition
            playVideo(videoUrl!!)
            player?.seekTo(lastPosition!!)

            // Dismiss the dialog
            dialog.dismiss()
        }

        // Initialize the AlertDialog using builder object
        dialog = builder.create()

        // Finally, display the alert dialog
        dialog.show()
    }

    private fun showQualityDialogYoutube() {
        // Late initialize an alert dialog object
        lateinit var dialog: AlertDialog

        // Initialize a new instance of alert dialog builder object
        val builder = AlertDialog.Builder(this, R.style.MyDialogTheme)

        // Set a title for alert dialog
        builder.setTitle("Choose a quality.")

        // Set the single choice items for alert dialog with initial selection
        builder.setSingleChoiceItems(youtubeLinksArray.map { it.format }.toTypedArray(), -1) { _, which ->
            // Get the dialog selected item
            val streamUrl = youtubeLinksArray[which].url

            // Change the layout background color using user selection
            videoUrl = streamUrl
            val lastPosition = player?.currentPosition
            playVideo(videoUrl!!)
            player?.seekTo(lastPosition!!)

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
        when (playbackState) {
            Player.STATE_IDLE -> Log.i(TAG, "playbackState: STATE_IDLE")
            Player.STATE_BUFFERING -> loadingView.visibility = View.VISIBLE
            Player.STATE_READY -> loadingView.visibility = View.GONE
            Player.STATE_ENDED -> toast("Video Ended")
            else -> {
            }
        }
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


    lateinit var tvRef: DatabaseReference
    var valueEventListener: ValueEventListener? = null
    private fun openGetConnectionCode() {
        val input = EditText(this@PlayerActivity)
//        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.maxLines = 1
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        input.layoutParams = lp

        val dialog = AlertDialog.Builder(this@PlayerActivity)
            .setView(input)
            .setTitle("Connection Code")
            .setMessage("please enter connection code.")
            .setIcon(R.drawable.logo)
            .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener { dialog1 ->

            val OkBtn = (dialog1 as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            OkBtn.setOnClickListener { view ->
                val connectionKey = input.text.toString()

                if (!TextUtils.isEmpty(connectionKey)) {

                    connectToTvDevice(connectionKey)

                    dialog.cancel()

                } else {
                    input.error = "Empty Connection Code"
                }
            }

            val cancel = dialog1.getButton(AlertDialog.BUTTON_NEGATIVE)
            cancel.setOnClickListener {
                dialog1.cancel()
            }
        }

        dialog.show()
    }

    private fun connectToTvDevice(connectionKey: String) {
        tvRef = FirebaseDatabase.getInstance().reference.child("TVs").child(connectionKey)
        //if screen connected or not

        valueEventListener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    tvRef.child(CONNECTED).setValue("1")
                    toast("Connected to $connectionKey")
                    openControlsActivity(connectionKey)
                    GalaxyCastApplication.getPreferenceHelper().code = connectionKey
                    player?.playWhenReady = false
                    tvRef.removeEventListener(this)
                    finish()
                } else {
                    tvRef.removeEventListener(this)

                    val dialog = AlertDialog.Builder(this@PlayerActivity)
                        .setTitle("Change Code?")
                        .setMessage("No device found with this code!\nType another code and try to connect.")
                        .setIcon(R.drawable.logo)
                        .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()

                    dialog.setOnShowListener { dialog1 ->

                        val OkBtn = (dialog1 as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                        OkBtn.setOnClickListener {
                            openGetConnectionCode()

                            dialog1.cancel()
                        }

                        val cancel = dialog1.getButton(AlertDialog.BUTTON_NEGATIVE)
                        cancel.setOnClickListener {
                            dialog1.cancel()
                        }
                    }

                    dialog.show()
                }

            }
        }

        tvRef.addValueEventListener(valueEventListener!!)

    }


    private fun openControlsActivity(connectionKey: String) {
        val intent = Intent(this@PlayerActivity, ControlsActivity::class.java)
        intent.putExtra("code", connectionKey)
        intent.putExtra("url", videoUrl)
        if (videoId != null) {
            intent.putExtra("image", getVideoImage(videoId!!))
        } else {
            intent.putExtra("image", video?.thumbnail)
        }
        intent.putExtra("playType", "single")
        intent.putExtra("position", player?.currentPosition)
        startActivity(intent)
        finish()
    }

    private fun getVideoImage(videoId: String): String {
        return "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
    }
}