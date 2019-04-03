package com.galaxycast.app.ui.search

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.chad.library.adapter.base.BaseQuickAdapter
import com.galaxycast.app.ui.player.PlayerActivity
import com.galaxycast.app.R
import com.galaxycast.app.ui.login.REQUEST_AUTHORIZATION
import com.galaxycast.app.utily.showGooglePlayServicesAvailabilityErrorDialog
import com.google.android.gms.cast.framework.*
import kotlinx.android.synthetic.main.activity_search.*



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

        viewModel.searchLiveData.observe(this, Observer { onSearchState(it) })

        mCastStateListener = CastStateListener { newState ->
            if (newState != CastState.NO_DEVICES_AVAILABLE) {
                showIntroductoryOverlay()
            }
        }

        mCastContext = CastContext.getSharedInstance(this);

        searchRv.adapter = adapter
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.search(query) }
                return false
            }

            override fun onQueryTextChange(query: String?): Boolean {
                query?.let {
                    if (query.isEmpty()) {
                        adapter.replaceData(emptyList())
                        searchRv.visibility = View.GONE
                        waitMessageTv.visibility = View.VISIBLE
                        waitView.visibility = View.VISIBLE
                    } else {
                        viewModel.search(query)
                    }
                }
                return false
            }
        })
        adapter.setOnLoadMoreListener({ viewModel.nextPage() }, searchRv)
        adapter.onItemClickListener =
            BaseQuickAdapter.OnItemClickListener { _, _, position ->
                adapter.data[position].id.let {
                    PlayerActivity.start(this, it.videoId)
                }
            }
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
            }
            SearchViewModel.SearchState.Error.Offline -> {
                searchRv.visibility = View.GONE
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
        getMenuInflater().inflate(R.menu.search_menu, menu);
        mediaRouteMenuItem = CastButtonFactory.setUpMediaRouteButton(
            getApplicationContext(), menu,
            R.id.search_cast_menu_item
        );
        return true
    }
}