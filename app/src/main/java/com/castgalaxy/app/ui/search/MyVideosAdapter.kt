package com.castgalaxy.app.ui.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.TimeUtils
import com.bumptech.glide.Glide
import com.castgalaxy.app.R
import com.castgalaxy.app.entity.MyVideos
import kotlinx.android.synthetic.main.search_item.view.*
import java.text.SimpleDateFormat
import java.util.*

class MyVideosAdapter(
    private val queriesList: List<MyVideos>,
    private val listener: (MyVideos) -> Unit
) : RecyclerView.Adapter<MyVideosAdapter.MyVideosViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        MyVideosViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.search_item,
                parent,
                false
            )
        )

    override fun getItemCount() = queriesList.size

    override fun onBindViewHolder(holder: MyVideosViewHolder, position: Int) =
        holder.bind(queriesList[position], listener)

    class MyVideosViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(myVideo: MyVideos, listener: (MyVideos) -> Unit) = with(itemView) {
            title.text = myVideo.title
            channelName.text = myVideo.channelName
            time.text = TimeUtils.millis2String(
                myVideo.time,
                SimpleDateFormat("yyyy-mm-dd", Locale.ENGLISH)
            )

            Glide.with(context)
                .load(myVideo.image)
                .into(image)

            setOnClickListener { listener(myVideo) }
        }
    }


}
