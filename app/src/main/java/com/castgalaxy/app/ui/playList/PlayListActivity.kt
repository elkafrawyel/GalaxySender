package com.castgalaxy.app.ui.playList

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.castgalaxy.app.R
import com.castgalaxy.app.entity.MyVideos
import com.castgalaxy.app.ui.playListPlayer.PlayListPlayerActivity
import com.castgalaxy.app.ui.search.MyVideosAdapter
import com.castgalaxy.app.utily.ObjectBox.Companion.boxStore
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.activity_play_list.*

class PlayListActivity : AppCompatActivity() {

    private var myVideos: List<MyVideos> = emptyList()
    private lateinit var adapter: MyVideosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play_list)


        val myVideosBox = boxStore.boxFor(MyVideos::class.java)
        myVideos = myVideosBox.all
        if (myVideos.isNotEmpty()) {
            myVideosRv.layoutManager = LinearLayoutManager(this)
            adapter = MyVideosAdapter(myVideos) { myVideos, index ->
                openVideoOptionForMyVideos(myVideos,index)

            }
            myVideosRv.adapter = adapter
        } else {
            toast(resources.getString(R.string.emptyPlayList
            ))
        }

        Fabric.with(this@PlayListActivity, Crashlytics())

    }

    private fun openVideoOptionForMyVideos(video: MyVideos, index: Int ) {

        val myVideoOptionsView = LayoutInflater.from(this).inflate(R.layout.myvideo_option, null, false)

        val dialog = AlertDialog.Builder(this)
            .setView(myVideoOptionsView)
            .setCancelable(true)
            .create()

        dialog.setOnShowListener {
            Glide.with(this@PlayListActivity)
                .load(video.image)
                .into(myVideoOptionsView.findViewById(R.id.videoOptionImg))

            myVideoOptionsView.findViewById<TextView>(R.id.playVideoTv).setOnClickListener {
                dialog.dismiss()
                PlayListPlayerActivity.start(this, video.videoId,index)
            }

            myVideoOptionsView.findViewById<TextView>(R.id.removeFromPlayListTv).setOnClickListener {
                dialog.dismiss()
                removeMyVideoFromPlayList(video)
            }
        }

        dialog.show()
    }

    private fun removeMyVideoFromPlayList(video: MyVideos) {
        val myVideosBoxStore = boxStore.boxFor(MyVideos::class.java)
        myVideosBoxStore.remove(video)
        (myVideos as ArrayList).remove(video)
        adapter.notifyDataSetChanged()
        toast(resources.getString(R.string.videoDeleted))
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
