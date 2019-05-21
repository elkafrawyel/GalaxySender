package com.castgalaxy.app.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import com.bumptech.glide.Glide
import com.castgalaxy.app.GalaxyCastApplication
import com.castgalaxy.app.R
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_controls.*

const val PLAY_TYPE = "playType"
const val PLAY = "play"
const val VIDEO_IMAGE = "videoImage"
const val URL = "url"
const val URL_LIST = "urlList"
const val LIST_POSITION = "listPosition"
const val SEEKUP = "seekUp"
const val SEEKDOWN = "seekDOWN"
const val SEEK = "seek"
const val SOUND_UP = "soundUp"
const val Sound_Down = "soundDOWN"
const val CONNECTED = "connected"

class ControlsActivity : AppCompatActivity() {

    lateinit var tvRef: DatabaseReference
    var valueEventListener: ValueEventListener? = null
    var image: String? = null
    var type: String? = null
    var url: String? = null
    var listPosition: Int? = 0
    var urlList = arrayListOf<String>()
    var playType: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_controls)

        val code = intent.getStringExtra("code")
        image = intent.getStringExtra("image")
        playType = intent.getStringExtra("playType")
        listPosition = intent.getIntExtra("listPosition", 0)
        type = intent.getStringExtra("type")
        val position = intent.getLongExtra("position", 0L)

        GalaxyCastApplication.getPreferenceHelper().image = image
        GalaxyCastApplication.getPreferenceHelper().type = playType
        GalaxyCastApplication.getPreferenceHelper().is_playing = playControl.tag == "1"

        Glide.with(this).load(image).into(videoBackdrop)

        tvRef = FirebaseDatabase.getInstance().reference.child("TVs").child(code)

        //add coming url
        if (playType == "single") {
            //video
            try {
                tvRef.child(PLAY_TYPE).setValue("single")
                url = intent.getStringExtra("url")
                tvRef.child(URL).setValue(url)
                tvRef.child(URL_LIST).setValue(null)
            } catch (e: java.lang.Exception) {

            }
        } else {
            //playList
            try {

                tvRef.child(PLAY_TYPE).setValue("multi")
                urlList = intent.getStringArrayListExtra("urlList")
                tvRef.child(URL_LIST).setValue(urlList)
                tvRef.child(LIST_POSITION).setValue(listPosition)
                tvRef.child(URL).setValue(null)
            } catch (e: Exception) {

            }
        }

        tvRef.child(PLAY).setValue("1")

        if (type == "resume") {

        } else {
            if (position != 0L)
                tvRef.child(SEEK).setValue(position.toString())
        }

        valueEventListener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val connectedValue = dataSnapshot.child(CONNECTED).value?.toString()
                if (connectedValue != null && connectedValue == "0") {
                    finish()
                }

                val play = dataSnapshot.child(PLAY).value.toString()
                if (play == "1") {
                    playControl.setImageDrawable(resources.getDrawable(R.drawable.ic_pause_circle_filled_white_24dp))
                    playControl.tag = "1"
                } else if (play == "0") {
                    playControl.setImageDrawable(resources.getDrawable(R.drawable.ic_play_circle_filled_white_24dp))
                    playControl.tag = "0"

                }

                val videoImageUrl = dataSnapshot.child(VIDEO_IMAGE).value?.toString()
                if (videoImageUrl != null && videoImageUrl != "") {
                    image = videoImageUrl
                    GalaxyCastApplication.getPreferenceHelper().image = image
                    Glide.with(GalaxyCastApplication.instance).load(videoImageUrl).into(videoBackdrop)
                }
            }
        }

        //if screen connected or not
        tvRef.addValueEventListener(valueEventListener!!)

        playControl.setOnClickListener {

            if (playControl.tag == "" || playControl.tag == "0") {
                tvRef.child(PLAY).setValue("1")
                playControl.tag = "1"
                playControl.setImageDrawable(resources.getDrawable(R.drawable.ic_pause_circle_filled_white_24dp))
            } else if (playControl.tag == "1") {
                playControl.tag = "0"
                tvRef.child(PLAY).setValue("0")
                playControl.setImageDrawable(resources.getDrawable(R.drawable.ic_play_circle_filled_white_24dp))
            } else {

            }
        }

        downSeekControl.setOnClickListener {
            val time = System.currentTimeMillis()
            tvRef.child(SEEKDOWN).setValue(time.toString())
            tvRef.child(SEEKUP).setValue("")
        }

        upSeekControl.setOnClickListener {
            val time = System.currentTimeMillis()
            tvRef.child(SEEKUP).setValue(time.toString())
            tvRef.child(SEEKDOWN).setValue("")

        }

        downSoundControl.setOnClickListener {
            val time = System.currentTimeMillis()
            tvRef.child(Sound_Down).setValue(time.toString())
            tvRef.child(SOUND_UP).setValue("")
        }

        upSoundControl.setOnClickListener {
            val time = System.currentTimeMillis()
            tvRef.child(SOUND_UP).setValue(time.toString())
            tvRef.child(Sound_Down).setValue("")

        }

        closeControl.setOnClickListener {
            finish()
        }
    }

    private fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onStart() {
        hideSystemUI()
        super.onStart()
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

}
