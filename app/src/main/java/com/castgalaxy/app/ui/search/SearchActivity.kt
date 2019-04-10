package com.castgalaxy.app.ui.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.castgalaxy.app.ui.player.PlayerActivity
import com.castgalaxy.app.R
import com.castgalaxy.app.entity.SearchQueries
import com.castgalaxy.app.entity.SearchQueries_
import com.castgalaxy.app.ui.login.REQUEST_AUTHORIZATION
import com.castgalaxy.app.utily.ObjectBox.Companion.boxStore
import com.castgalaxy.app.utily.showGooglePlayServicesAvailabilityErrorDialog
import com.google.android.gms.cast.framework.*
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_search.*
import androidx.appcompat.app.AlertDialog
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.castgalaxy.app.entity.MyVideos
import com.castgalaxy.app.entity.MyVideos_
import com.castgalaxy.app.ui.playList.PlayListActivity
import com.crashlytics.android.Crashlytics
import com.google.api.services.youtube.model.SearchResult
import io.fabric.sdk.android.Fabric


class SearchActivity : AppCompatActivity() {

    private val TAG = "GalaxyCast"

    private var mCastContext: CastContext? = null
    private var mediaRouteMenuItem: MenuItem? = null
    private var mIntroductoryOverlay: IntroductoryOverlay? = null
    private var mCastStateListener: CastStateListener? = null


    private val viewModel: SearchViewModel by lazy { ViewModelProviders.of(this).get(SearchViewModel::class.java) }
    private val adapter = SearchAdapter().apply {
        setEnableLoadMore(true)
        openLoadAnimation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setupActionBar()

//        var s: String = "A"
//        val a: String? = null
//        s = a!!

        viewModel.searchLiveData.observe(this, Observer { onSearchState(it) })

        mCastStateListener = CastStateListener { newState ->
            if (newState != CastState.NO_DEVICES_AVAILABLE) {
                showIntroductoryOverlay()
            }
        }

        mCastContext = CastContext.getSharedInstance(this);

        searchRv.adapter = adapter

        if (!searchView.isFocused) {
            searchView.clearFocus();
        }

        searchView.setOnClickListener {
            getSearchQueries()
        }


        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null && query.isNotEmpty()) {
                    val queriesBox = boxStore.boxFor(SearchQueries::class.java)
                    val stored = queriesBox.query().equal(SearchQueries_.text, query).build().find()
                    if (stored.size == 0)
                        queriesBox.put(SearchQueries(0, query))
                    else
                        toast(
                            "exists"
                        )
                }
                query?.let { viewModel.search(query) }
                queriesRv.visibility = View.GONE
                searchRv.requestFocus()
                return false
            }

            override fun onQueryTextChange(query: String?): Boolean {
                query?.let {
                    viewModel.stopSearch()
                    adapter.replaceData(emptyList())
                    searchRv.visibility = View.GONE
                    loadingView.visibility = View.GONE
                    waitMessageTv.visibility = View.VISIBLE
                    waitView.visibility = View.VISIBLE
                    if (query.isEmpty()) {
                        queriesRv.visibility = View.GONE
                    } else {
                        getSearchQueries()
                    }
                }
                return false
            }
        })

        adapter.setOnLoadMoreListener({ viewModel.nextPage() }, searchRv)
        adapter.onItemClickListener =
            BaseQuickAdapter.OnItemClickListener { _, _, position ->
                showVideoOptions(adapter.data[position])
            }

        fabBtn.setOnClickListener { openBottomSheet() }

        Fabric.with(this@SearchActivity, Crashlytics())

    }

    private fun showVideoOptions(searchResult: SearchResult) {

        val videoOptionsView = LayoutInflater.from(this).inflate(R.layout.video_option, null, false)

        val dialog = AlertDialog.Builder(this)
            .setView(videoOptionsView)
            .setCancelable(true)
            .create()

        dialog.setOnShowListener {
            Glide.with(this@SearchActivity)
                .load(searchResult.snippet.thumbnails.medium.url)
                .into(videoOptionsView.findViewById(R.id.videoOptionImg))

            videoOptionsView.findViewById<TextView>(R.id.playVideoTv).setOnClickListener {
                dialog.dismiss()
                PlayerActivity.start(this, searchResult.id.videoId, null)
            }

            videoOptionsView.findViewById<TextView>(R.id.addToPlayListTv).setOnClickListener {
                dialog.dismiss()
                addVideoToPlaylist(searchResult)
            }
        }

        dialog.show()
    }

    private fun addVideoToPlaylist(searchResult: SearchResult) {
        val videosBox = boxStore.boxFor(MyVideos::class.java)
        val stored = videosBox.query().equal(MyVideos_.videoId, searchResult.id.videoId).build().find()
        if (stored.size == 0) {
            videosBox.put(
                MyVideos(
                    id = 0,
                    videoId = searchResult.id.videoId,
                    title = searchResult.snippet.title,
                    channelName = searchResult.snippet.channelTitle,
                    time = searchResult.snippet.publishedAt.value,
                    image = searchResult.snippet.thumbnails.medium.url
                )
            )
            toast("Video added to playlist.")
        } else {
            toast("Video is in your playlist.")
        }
    }

    private fun openBottomSheet() {
        val bottomSheet = BottomSheetDialog(this)
        val bottomSheetView = LayoutInflater.from(this).inflate(R.layout.serach_bottom_sheet, null, false)
        bottomSheet.setContentView(bottomSheetView)
        bottomSheetView.findViewById<TextView>(R.id.playlistTv).setOnClickListener {
            bottomSheet.dismiss()
            openPlaylist()
        }

        bottomSheetView.findViewById<TextView>(R.id.addUrlTv).setOnClickListener {
            bottomSheet.dismiss()
            openAddUrl()
        }

        bottomSheetView.findViewById<TextView>(R.id.cancelMbtn).setOnClickListener {
            bottomSheet.dismiss()
        }

        bottomSheetView.findViewById<LinearLayout>(R.id.allowedUrls).setOnClickListener {
            bottomSheet.dismiss()
            openAddUrl()
        }

        bottomSheet.show()
    }

    private fun openAddUrl() {
        val input = EditText(this);
        input.hint = "Add a media url to play"
        input.gravity = Gravity.CENTER
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        input.layoutParams = lp

        val dialog = AlertDialog.Builder(this)
            .setView(input)
            .setTitle("Add Url")
            .setIcon(R.drawable.logo)
            .setCancelable(true)
            .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.setOnClickListener {
                val link = input.text.toString()

                if (link.isNotEmpty()) {
                    PlayerActivity.start(this, null, link)
                    dialog.dismiss()
                } else {
                    input.error = "Empty Url Not Allowed"
                }
            }

            val cancel = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            cancel.setOnClickListener {
                dialog.cancel()
            }
        }

        dialog.show()
    }

    private fun openPlaylist() {
        val intent = Intent(this@SearchActivity, PlayListActivity::class.java)
        startActivity(intent)
    }


    private fun getSearchQueries() {
        val query = boxStore.boxFor(SearchQueries::class.java)
        val queriesList = query.all.takeLast(10).reversed()
        if (queriesList.isNotEmpty()) {
            queriesRv.visibility = View.VISIBLE
            queriesRv.layoutManager = LinearLayoutManager(this)
            queriesRv.adapter = QueriesAdapter(queriesList) {
                searchView.setQuery(it.text, true)
                queriesRv.visibility = View.GONE
            }
        } else {
            queriesRv.visibility = View.GONE
        }
    }

    private fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun setupActionBar() {
        toolbar.setTitle(R.string.app_name)
        setSupportActionBar(toolbar)
    }

    override fun onResume() {
        mCastContext!!.addCastStateListener(mCastStateListener)
        super.onResume()
    }

    override fun onPause() {
        mCastContext!!.removeCastStateListener(mCastStateListener)
        super.onPause()
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
                    .setTitleText("Galaxy Cast\nPlease press here if you want to cast")
                    .setSingleTime()
                    .setOnOverlayDismissedListener { mIntroductoryOverlay = null }
                    .build()
                mIntroductoryOverlay!!.show()
            }
        }
    }

    private fun onSearchState(state: SearchViewModel.SearchState) {
        when (state) {
            SearchViewModel.SearchState.Loading -> {
                searchRv.visibility = View.GONE
                loadingView.visibility = View.VISIBLE
                waitMessageTv.visibility = View.GONE
                waitView.visibility = View.GONE
            }
            is SearchViewModel.SearchState.Success -> {
                adapter.replaceData(state.data)
                loadingView.visibility = View.GONE
                waitMessageTv.visibility = View.GONE
                waitView.visibility = View.GONE
                searchRv.visibility = View.VISIBLE
            }
            is SearchViewModel.SearchState.NextPage -> {
                adapter.addData(state.data)
                adapter.loadMoreComplete()
            }
            is SearchViewModel.SearchState.Ended -> {
                adapter.addData(state.data)
                adapter.loadMoreEnd()
            }
            SearchViewModel.SearchState.Error.Unknown -> {
                searchRv.visibility = View.GONE
                loadingView.visibility = View.GONE
                waitView.visibility = View.GONE
                waitMessageTv.visibility = View.GONE
                Toast.makeText(this, "Youtube search is not working.", Toast.LENGTH_LONG).show()
            }
            SearchViewModel.SearchState.Error.Offline -> {
                searchRv.visibility = View.GONE
                loadingView.visibility = View.GONE
                waitView.visibility = View.GONE
                waitMessageTv.visibility = View.GONE
                Toast.makeText(this, "No Internet Connection.", Toast.LENGTH_LONG).show()
            }
            is SearchViewModel.SearchState.Error.GooglePlay -> {
                searchRv.visibility = View.GONE
                showGooglePlayServicesAvailabilityErrorDialog(state.connectionStatusCode)
            }
            is SearchViewModel.SearchState.Error.UserRecoverableAuth -> {
                searchRv.visibility = View.GONE
                startActivityForResult(
                    state.intent,
                    REQUEST_AUTHORIZATION
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu);
        menuInflater.inflate(R.menu.search_menu, menu);
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(
            applicationContext, menu,
            R.id.search_cast_menu_item
        );
        return true
    }
}