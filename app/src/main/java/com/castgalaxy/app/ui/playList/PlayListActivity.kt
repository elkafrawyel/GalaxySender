package com.castgalaxy.app.ui.playList

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.castgalaxy.app.R
import com.castgalaxy.app.entity.MyVideos
import com.castgalaxy.app.ui.playListPlayer.PlayListPlayerActivity
import com.castgalaxy.app.ui.player.PlayerActivity
import com.castgalaxy.app.ui.search.MyVideosAdapter
import com.castgalaxy.app.utily.ObjectBox.Companion.boxStore
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
            adapter = MyVideosAdapter(myVideos) {
                openVideoOptionForMyVideos(it)
            }
            myVideosRv.adapter = adapter
        } else {
            toast("Your playlist is empty.")
        }
    }

    private fun openVideoOptionForMyVideos(myVideos: MyVideos) {

        val myVideoOptionsView = LayoutInflater.from(this).inflate(R.layout.myvideo_option, null, false)

        val dialog = AlertDialog.Builder(this)
            .setView(myVideoOptionsView)
            .setCancelable(true)
            .create()

        dialog.setOnShowListener {
            Glide.with(this@PlayListActivity)
                .load(myVideos.image)
                .into(myVideoOptionsView.findViewById(R.id.videoOptionImg))

            myVideoOptionsView.findViewById<TextView>(R.id.playVideoTv).setOnClickListener {
                dialog.dismiss()
                PlayListPlayerActivity.start(this, myVideos.videoId)
            }

            myVideoOptionsView.findViewById<TextView>(R.id.removeFromPlayListTv).setOnClickListener {
                dialog.dismiss()
                removeMyVideoFromPlayList(myVideos)
            }
        }

        dialog.show()
    }

    private fun removeMyVideoFromPlayList(video: MyVideos) {
        val myVideosBoxStore = boxStore.boxFor(MyVideos::class.java)
        myVideosBoxStore.remove(video)
        (myVideos as ArrayList).remove(video)
        adapter.notifyDataSetChanged()
        toast("Video deleted.")
    }

    private fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }


}
