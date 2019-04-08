package com.castgalaxy.app.ui.search

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.castgalaxy.app.R
import com.castgalaxy.app.entity.SearchQueries
import kotlinx.android.synthetic.main.search_queries.view.*

class QueriesAdapter(
    private val queriesList: List<SearchQueries>,
    private val listener: (SearchQueries) -> Unit
) :
    RecyclerView.Adapter<QueriesAdapter.QueriesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        QueriesViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.search_queries,
                parent,
                false
            )
        )

    override fun getItemCount() = queriesList.size

    override fun onBindViewHolder(holder: QueriesViewHolder, position: Int) =
        holder.bind(queriesList[position], listener)

    class QueriesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(query: SearchQueries, listener: (SearchQueries) -> Unit) = with(itemView) {
            queryText.text = query.text
            setOnClickListener { listener(query) }
        }
    }
}