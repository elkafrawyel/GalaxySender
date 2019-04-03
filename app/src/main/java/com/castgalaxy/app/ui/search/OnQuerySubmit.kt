package com.castgalaxy.app.ui.search

import androidx.appcompat.widget.SearchView

abstract class OnQuerySubmit : SearchView.OnQueryTextListener {
    abstract fun onQuerySubmit(query: String)
    override fun onQueryTextSubmit(query: String?): Boolean {
        query?.let { onQuerySubmit(query) }
        return false
    }
    override fun onQueryTextChange(newText: String?): Boolean {
        return false
    }
}