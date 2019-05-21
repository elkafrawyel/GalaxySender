package com.castgalaxy.app.ui.search

import com.blankj.utilcode.util.TimeUtils
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.castgalaxy.app.R
import com.google.api.services.youtube.model.SearchResult
import java.text.SimpleDateFormat
import java.util.*

class SearchAdapter : BaseQuickAdapter<SearchResult, BaseViewHolder>(R.layout.search_item, ArrayList()) {

    override fun convert(helper: BaseViewHolder, result: SearchResult) {
        helper.setText(R.id.title, result.snippet.title)
            .setText(R.id.channelName, result.snippet.channelTitle)
            .setText(
                R.id.time,
                TimeUtils.millis2String(result.snippet.publishedAt.value, SimpleDateFormat("yyyy-mm-dd", Locale.ENGLISH))
            )

        Glide.with(mContext)
            .load(result.snippet.thumbnails.medium.url)
            .into(helper.getView(R.id.image))
    }

}

//class DiffCallBack : DiffUtil.ItemCallback<SearchResultSnippet>() {
//    override fun areItemsTheSame(oldItem: SearchResultSnippet, newItem: SearchResultSnippet): Boolean {
//        return true
//    }
//
//    override fun areContentsTheSame(oldItem: SearchResultSnippet, newItem: SearchResultSnippet): Boolean {
//        return oldItem == newItem
//    }
//
//}
//
//class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
//
//    private val image = itemView.image
//    private val title = itemView.title
//    private val channelName = itemView.channelName
//    private val time = itemView.time
//
//    fun bind(snippet: SearchResultSnippet) {
//        Glide.with(itemView).load(snippet.thumbnails.medium.urlList).into(image)
//        Log.i("image", "height: ${snippet.thumbnails.medium.height} width: ${snippet.thumbnails.medium.width}")
//        title.text = snippet.title
//        channelName.text = snippet.channelTitle
//        time.text = snippet.publishedAt.toStringRfc3339()
//    }
//
//}