package com.castgalaxy.app.ui.search

import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.castgalaxy.app.R
import com.castgalaxy.app.entity.YoutubeVideoResponse
import java.util.*

class SearchAdapter : BaseQuickAdapter<YoutubeVideoResponse, BaseViewHolder>(R.layout.search_item, ArrayList()) {

    override fun convert(helper: BaseViewHolder, result: YoutubeVideoResponse) {
        helper.setText(R.id.title, result.videotitle)
            .setText(R.id.channelName, result.channeltitle)
            .setText(R.id.time,result.videodate)
            .setText(R.id.length, result.duration)
            .setText(R.id.quality,result.quality)
        Glide.with(mContext)
            .load(result.imghigh)
            .into(helper.getView(R.id.image))
    }

}
