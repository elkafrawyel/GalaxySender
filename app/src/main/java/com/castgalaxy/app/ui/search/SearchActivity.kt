package com.castgalaxy.app.ui.search

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.castgalaxy.app.ui.player.PlayerActivity
import com.castgalaxy.app.R
import com.castgalaxy.app.utily.ObjectBox.Companion.boxStore
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.android.synthetic.main.activity_search.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.blankj.utilcode.util.AppUtils
import com.bumptech.glide.Glide
import com.castgalaxy.app.GalaxyCastApplication
import com.castgalaxy.app.entity.*
import com.castgalaxy.app.ui.ControlsActivity
import com.castgalaxy.app.ui.PLAY
import com.castgalaxy.app.ui.URL
import com.castgalaxy.app.ui.login.ActivationActivity
import com.castgalaxy.app.ui.playList.PlayListActivity
import com.castgalaxy.app.ui.player.CONNECTED
import com.crashlytics.android.Crashlytics
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.firebase.database.*
import io.fabric.sdk.android.Fabric
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList


class SearchActivity : AppCompatActivity() {

    private val TAG = "GalaxyCast"
    private val LOG_TAG = "Barcode Scanner API"
    private val PHOTO_REQUEST = 10
    private var detector: BarcodeDetector? = null
    private var imageUri: Uri? = null
    private val REQUEST_WRITE_PERMISSION = 20
    private val SAVED_INSTANCE_URI = "uri"
    private var currImagePath: String? = null
    internal var imageFile: File? = null


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

        changeLanguage(GalaxyCastApplication.getPreferenceHelper().language)

        if (savedInstanceState != null) {
            imageUri = Uri.parse(savedInstanceState.getString(SAVED_INSTANCE_URI))
        }
        detector = BarcodeDetector.Builder(applicationContext)
            .setBarcodeFormats(Barcode.ALL_FORMATS)
            .build()
        if (!detector!!.isOperational) {
            toast(resources.getString(R.string.barCodeSetUp))
            return
        }

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


    private fun showVideoOptions(youtubeVideoResponse: YoutubeVideoResponse) {

        val videoOptionsView = LayoutInflater.from(this).inflate(R.layout.video_option, null, false)

        val dialog = AlertDialog.Builder(this)
            .setView(videoOptionsView)
            .setCancelable(true)
            .create()

        dialog.setOnShowListener {
            Glide.with(this@SearchActivity)
                .load(youtubeVideoResponse.imghigh)
                .into(videoOptionsView.findViewById(R.id.videoOptionImg))

            videoOptionsView.findViewById<TextView>(R.id.playVideoTv).setOnClickListener {
                dialog.dismiss()
                PlayerActivity.start(this, youtubeVideoResponse.videoid, null)
            }

            videoOptionsView.findViewById<TextView>(R.id.addToPlayListTv).setOnClickListener {
                dialog.dismiss()
                addVideoToPlaylist(youtubeVideoResponse)
            }
        }

        dialog.show()
    }

    private fun addVideoToPlaylist(youtubeVideoResponse: YoutubeVideoResponse) {
        val videosBox = boxStore.boxFor(MyVideos::class.java)
        val stored = videosBox.query().equal(MyVideos_.videoId, youtubeVideoResponse.videoid!!).build().find()
        if (stored.size == 0) {
            videosBox.put(
                MyVideos(
                    id = 0,
                    videoId = youtubeVideoResponse.videoid,
                    title = youtubeVideoResponse.videotitle ?: "",
                    channelName = youtubeVideoResponse.channeltitle ?: "",
                    time = youtubeVideoResponse.videodate ?: "",
                    image = youtubeVideoResponse.imghigh!!
                )
            )
            toast(resources.getString(R.string.videoAddedToPlaylist))
        } else {
            toast(resources.getString(R.string.videoIsAddedToPlaylist))
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
        input.hint = resources.getString(R.string.addMediaUrl)
        input.gravity = Gravity.CENTER
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        input.layoutParams = lp

        val dialog = AlertDialog.Builder(this)
            .setView(input)
            .setTitle(resources.getString(R.string.addUrl))
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
                    input.error = resources.getString(R.string.empty_Url)
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
        super.onResume()
        val code = GalaxyCastApplication.getPreferenceHelper().code
        val image = GalaxyCastApplication.getPreferenceHelper().image
        val type = GalaxyCastApplication.getPreferenceHelper().type
        Glide.with(this@SearchActivity).load(image).into(videoImage)
        var url: String? = null

        if (code != null) {

            tvRef = FirebaseDatabase.getInstance().reference.child("TVs").child(code)
            //if screen connected or not

            valueEventListener = object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    tvRef.removeEventListener(this)
                    if (dataSnapshot.exists()) {
                        val connected = dataSnapshot.child(CONNECTED).value
                        url = dataSnapshot.child(URL).value.toString()
                        if (connected == "1") {
                            //change menu

                            if (image != null && url != null) {
                                smallVideo.visibility = View.VISIBLE
                                val play = dataSnapshot.child(PLAY).value
                                if (play == "1") {
                                    searchPlayPause.tag = "1"
                                    searchPlayPause.setImageDrawable(resources.getDrawable(R.drawable.ic_pause_circle_filled_white_24dp))
                                } else if (searchPlayPause.tag == "1") {
                                    searchPlayPause.tag = "0"
                                    searchPlayPause.setImageDrawable(resources.getDrawable(R.drawable.ic_play_circle_filled_white_24dp))
                                } else {

                                }
                            }
                        } else {
                            tvRef.removeEventListener(this)
                            smallVideo.visibility = View.GONE
                        }
                    } else {
                        tvRef.removeEventListener(this)
                        smallVideo.visibility = View.GONE
                    }
                }
            }

            if (tvRef != null)
                tvRef.addValueEventListener(valueEventListener!!)
        }
        searchPlayPause.setOnClickListener {
            if (searchPlayPause.tag == "" || searchPlayPause.tag == "0") {
                tvRef.child(PLAY).setValue("1")
                searchPlayPause.tag = "1"
                searchPlayPause.setImageDrawable(resources.getDrawable(R.drawable.ic_pause_circle_filled_white_24dp))
            } else if (searchPlayPause.tag == "1") {
                searchPlayPause.tag = "0"
                tvRef.child(PLAY).setValue("0")
                searchPlayPause.setImageDrawable(resources.getDrawable(R.drawable.ic_play_circle_filled_white_24dp))
            } else {

            }
        }

        smallVideo.setOnClickListener {
            if (valueEventListener != null) {
                tvRef.removeEventListener(valueEventListener!!)
                openControlsActivity(code, image, url!!, type)
            }
        }

    }

    private fun openControlsActivity(connectionKey: String, image: String, url: String, type: String) {
        val intent = Intent(this@SearchActivity, ControlsActivity::class.java)
        intent.putExtra("code", connectionKey)
        intent.putExtra("url", url)
        intent.putExtra("image", image)
        intent.putExtra("position", 0L)
        intent.putExtra("playType", type)
        intent.putExtra("type", "resume")
        startActivity(intent)
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
                state.data?.let { adapter.replaceData(it) }
                loadingView.visibility = View.GONE
                waitMessageTv.visibility = View.GONE
                waitView.visibility = View.GONE
                searchRv.visibility = View.VISIBLE
            }
            is SearchViewModel.SearchState.NextPage -> {
                state.data?.let { adapter.addData(it) }
                adapter.loadMoreComplete()
            }
            is SearchViewModel.SearchState.Ended -> {
                state.data?.let { adapter.addData(it) }
                adapter.loadMoreEnd()
            }
            SearchViewModel.SearchState.Error.Unknown -> {
                searchRv.visibility = View.GONE
                loadingView.visibility = View.GONE
                waitView.visibility = View.GONE
                waitMessageTv.visibility = View.GONE
                Toast.makeText(this, resources.getString(R.string.search_not_working), Toast.LENGTH_LONG).show()
            }
            SearchViewModel.SearchState.Error.Offline -> {
                searchRv.visibility = View.GONE
                loadingView.visibility = View.GONE
                waitView.visibility = View.GONE
                waitMessageTv.visibility = View.GONE
                Toast.makeText(this, resources.getString(R.string.noConnection), Toast.LENGTH_LONG).show()
            }

        }
    }

    var isConnected = false
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        super.onCreateOptionsMenu(menu);
        menuInflater.inflate(R.menu.search_menu, menu);
        val code = GalaxyCastApplication.getPreferenceHelper().code
        if (code != null) {

            tvRef = FirebaseDatabase.getInstance().reference.child("TVs").child(code)
            //if screen connected or not

            valueEventListener = object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {

                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val connected = dataSnapshot.child(CONNECTED).value
                        if (connected == "1") {
                            //change menu
                            menu?.findItem(R.id.player_connect)?.icon =
                                ContextCompat.getDrawable(this@SearchActivity, R.drawable.ic_cast_connected_white_24dp);
                            isConnected = true

                        } else {
                            smallVideo.visibility = View.GONE
                            menu?.findItem(R.id.player_connect)?.icon =
                                ContextCompat.getDrawable(this@SearchActivity, R.drawable.ic_cast_white_24dp);
                            isConnected = false
                        }
                    }
                }
            }

            tvRef.addValueEventListener(valueEventListener!!)
        } else {
            menu?.findItem(R.id.player_connect)?.icon =
                resources.getDrawable(R.drawable.ic_cast_white_24dp)
            isConnected = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.player_connect) {
            val code = GalaxyCastApplication.getPreferenceHelper().code

            if (isConnected) {
                val dialog = AlertDialog.Builder(this@SearchActivity)
                    .setTitle(resources.getString(R.string.disconnected))
                    .setIcon(R.drawable.logo)
                    .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                    .setNegativeButton(android.R.string.cancel, null)
                    .create()

                dialog.setOnShowListener { dialog1 ->

                    val OkBtn = (dialog1 as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                    OkBtn.setOnClickListener {
                        tvRef.child(CONNECTED).setValue("0")
                        toast(resources.getString(R.string.disconnectedFrom) + " " + code)
                        smallVideo.visibility = View.GONE
                        item.icon = ContextCompat.getDrawable(this, R.drawable.ic_cast_white_24dp);
                        dialog1.cancel()
                    }

                    val cancel = dialog1.getButton(AlertDialog.BUTTON_NEGATIVE)
                    cancel.setOnClickListener {
                        dialog1.cancel()
                    }
                }

                dialog.show()
            } else {
                if (code != null) {
                    connectToTvDevice(code)
                } else {
                    openGetConnectionCode()
                }
            }
        } else if (item?.itemId == R.id.action_clear_history) {
            val queryBox = boxStore.boxFor(SearchQueries::class.java)
            queryBox.removeAll()
            toast(resources.getString(R.string.clearHistory))
        } else if (item?.itemId == R.id.action_reset_code) {
            val input = EditText(this@SearchActivity)
            input.maxLines = 1
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            input.layoutParams = lp

            val dialog = AlertDialog.Builder(this@SearchActivity)
                .setView(input)
                .setTitle(resources.getString(R.string.connection_code))
                .setMessage(resources.getString(R.string.please_type_code))
                .setIcon(R.drawable.logo)
                .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                .setNegativeButton(android.R.string.cancel, null)
                .create()

            dialog.setOnShowListener { dialog1 ->

                val OkBtn = (dialog1 as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                OkBtn.setOnClickListener { view ->
                    val connectionKey = input.text.toString()

                    if (!TextUtils.isEmpty(connectionKey)) {
                        //Store the code

                        GalaxyCastApplication.getPreferenceHelper().code = connectionKey

                        dialog.cancel()

                    } else {
                        input.error = resources.getString(R.string.empty_code)
                    }
                }

                val cancel = dialog1.getButton(AlertDialog.BUTTON_NEGATIVE)
                cancel.setOnClickListener {
                    dialog1.cancel()
                }
            }

            dialog.show()
        } else if (item?.itemId == R.id.player_scan) {
            ActivityCompat.requestPermissions(
                this@SearchActivity,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission_group.CAMERA,
                    Manifest.permission.CAMERA
                ), REQUEST_WRITE_PERMISSION
            )
        } else if (item?.itemId == R.id.action_language) {
            openChangeLanguage()
        } else if (item?.itemId == R.id.action_auto_play) {
            openAutoPlayDialog()
        } else if (item?.itemId == R.id.action_info) {
            openInfoDialog()
        } else if (item?.itemId == R.id.action_logOut) {
            logOut()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun logOut() {
        GalaxyCastApplication.getPreferenceHelper().clear()
        val intent = Intent(this@SearchActivity, ActivationActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun openInfoDialog() {

        val infoView = LayoutInflater.from(this).inflate(R.layout.info_view, null, false)

        val dialog = AlertDialog.Builder(this)
            .setView(infoView)
            .setCancelable(true)
            .create()

        dialog.setOnShowListener {

            infoView.findViewById<TextView>(R.id.info_expires).text =
                resources.getString(R.string.date) + " - " + GalaxyCastApplication.getPreferenceHelper().date

            infoView.findViewById<TextView>(R.id.info_licence).text =
                resources.getString(R.string.licence) + " - " + GalaxyCastApplication.getPreferenceHelper().licence

            infoView.findViewById<TextView>(R.id.info_version).text =
                resources.getString(R.string.version) + " - " + AppUtils.getAppVersionName()
        }

        dialog.show()
    }

    private fun openAutoPlayDialog() {
        // Late initialize an alert dialog object
        lateinit var dialog: AlertDialog

        // Initialize a new instance of alert dialog builder object
        val builder = AlertDialog.Builder(this, R.style.MyDialogTheme)

        // Set a title for alert dialog
        builder.setTitle(resources.getString(R.string.autoPlayList))
        val options = ArrayList<String>()
        options.add(resources.getString(R.string.yes))
        options.add(resources.getString(R.string.no))

        var index = 0
        if (!GalaxyCastApplication.getPreferenceHelper().autoPlay) {
            index = 1
        }

        // Set the single choice items for alert dialog with initial selection
        builder.setSingleChoiceItems(options.toTypedArray(), index) { _, which ->
            // Get the dialog selected item
            GalaxyCastApplication.getPreferenceHelper().autoPlay = which == 0
            // Dismiss the dialog
            dialog.dismiss()
        }

        // Initialize the AlertDialog using builder object
        dialog = builder.create()

        // Finally, display the alert dialog
        dialog.show()
    }

    private fun openChangeLanguage() {
        // Late initialize an alert dialog object
        lateinit var dialog: AlertDialog

        // Initialize a new instance of alert dialog builder object
        val builder = AlertDialog.Builder(this, R.style.MyDialogTheme)

        // Set a title for alert dialog
        builder.setTitle(resources.getString(R.string.chooseLanguage))
        val langs = ArrayList<String>()
        val lang = ArrayList<String>()
        langs.add("English")
        langs.add("Spanish")
        langs.add("Portuguese")

        lang.add("en")
        lang.add("es")
        lang.add("pt")
        // Set the single choice items for alert dialog with initial selection
        builder.setSingleChoiceItems(langs.toTypedArray(), -1) { _, which ->
            // Get the dialog selected item
            saveLanguage(language = lang[which])
            changeLanguage(lang[which])
            finish()
            restartApplication()
            // Dismiss the dialog
            dialog.dismiss()
        }

        // Initialize the AlertDialog using builder object
        dialog = builder.create()

        // Finally, display the alert dialog
        dialog.show()
    }

    lateinit var tvRef: DatabaseReference
    var valueEventListener: ValueEventListener? = null
    private fun openGetConnectionCode() {
        val input = EditText(this@SearchActivity)
//        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.maxLines = 1
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        input.layoutParams = lp

        val dialog = AlertDialog.Builder(this@SearchActivity)
            .setView(input)
            .setTitle(resources.getString(R.string.connection_code))
            .setMessage(resources.getString(R.string.please_type_code))
            .setIcon(R.drawable.logo)
            .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener { dialog1 ->

            val OkBtn = (dialog1 as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            OkBtn.setOnClickListener { view ->
                val connectionKey = input.text.toString()

                if (!TextUtils.isEmpty(connectionKey)) {
                    //Store the code

                    connectToTvDevice(connectionKey)

                    dialog.cancel()

                } else {
                    input.error = resources.getString(R.string.empty_code)
                }
            }

            val cancel = dialog1.getButton(AlertDialog.BUTTON_NEGATIVE)
            cancel.setOnClickListener {
                dialog1.cancel()
            }
        }

        dialog.show()
    }

    private fun connectToTvDevice(connectionKey: String) {
        tvRef = FirebaseDatabase.getInstance().reference.child("TVs").child(connectionKey)
        //if screen connected or not

        valueEventListener = object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    tvRef.child(CONNECTED).setValue("1")
                    GalaxyCastApplication.getPreferenceHelper().code = connectionKey
                    toast(resources.getString(R.string.connectedTo) + connectionKey)
                    tvRef.removeEventListener(this)
                } else {
                    tvRef.removeEventListener(this)
                    val dialog = AlertDialog.Builder(this@SearchActivity)
                        .setTitle(resources.getString(R.string.change_code))
                        .setMessage(resources.getString(R.string.no_device_available))
                        .setIcon(R.drawable.logo)
                        .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()

                    dialog.setOnShowListener { dialog1 ->

                        val OkBtn = (dialog1 as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                        OkBtn.setOnClickListener {
                            openGetConnectionCode()

                            dialog1.cancel()
                        }

                        val cancel = dialog1.getButton(AlertDialog.BUTTON_NEGATIVE)
                        cancel.setOnClickListener {
                            dialog1.cancel()
                        }
                    }

                    dialog.show()
                }

            }
        }

        tvRef.addValueEventListener(valueEventListener!!)

    }

    //================================== Code  ========================================================================
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_WRITE_PERMISSION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture()
            } else {
                Toast.makeText(this@SearchActivity, resources.getString(R.string.permissionDenied), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PHOTO_REQUEST && resultCode == Activity.RESULT_OK) {
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = imageUri
            launchMediaScanIntent(mediaScanIntent)
            try {
                val bitmap = decodeBitmapUri(this, imageUri)
                if (detector!!.isOperational && bitmap != null) {
                    val frame = Frame.Builder().setBitmap(bitmap).build()
                    val barcodes = detector!!.detect(frame)
                    for (index in 0 until barcodes.size()) {
                        val code = barcodes.valueAt(index)
                        val validCoded = code.displayValue.replace("'", "").replace(".", "")
                        toast(validCoded)
                        openScanCode(validCoded)
                        val type = barcodes.valueAt(index).valueFormat
                        when (type) {
                            Barcode.CONTACT_INFO -> Log.i(LOG_TAG, code.contactInfo.title)
                            Barcode.EMAIL -> Log.i(LOG_TAG, code.email.address)
                            Barcode.ISBN -> Log.i(LOG_TAG, code.rawValue)
                            Barcode.PHONE -> Log.i(LOG_TAG, code.phone.number)
                            Barcode.PRODUCT -> Log.i(LOG_TAG, code.rawValue)
                            Barcode.SMS -> Log.i(LOG_TAG, code.sms.message)
                            Barcode.TEXT -> Log.i(LOG_TAG, code.rawValue)
                            Barcode.URL -> Log.i(LOG_TAG, "url: " + code.url.url)
                            Barcode.WIFI -> Log.i(LOG_TAG, code.wifi.ssid)
                            Barcode.GEO -> Log.i(LOG_TAG, code.geoPoint.lat.toString() + ":" + code.geoPoint.lng)
                            Barcode.CALENDAR_EVENT -> Log.i(LOG_TAG, code.calendarEvent.description)
                            Barcode.DRIVER_LICENSE -> Log.i(LOG_TAG, code.driverLicense.licenseNumber)
                            else -> Log.i(LOG_TAG, code.rawValue)
                        }
                    }
                    if (barcodes.size() == 0) {
                        toast(resources.getString(R.string.scan_failed))
                    }
                } else {
                    toast(resources.getString(R.string.barCodeSetUp))
                }
            } catch (e: Exception) {
                toast(resources.getString(R.string.loadImageFailed))
            }

        }
    }

    private fun openScanCode(displayValue: String) {

        val dialog = AlertDialog.Builder(this@SearchActivity)
            .setTitle(resources.getString(R.string.connection_code))
            .setMessage(resources.getString(R.string.youWantToConnect) + "( " + displayValue + " )")
            .setIcon(R.drawable.logo)
            .setPositiveButton(android.R.string.ok, null) //Set to null. We override the onclick
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.setOnShowListener { dialog1 ->

            val okBtn = (dialog1 as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
            okBtn.setOnClickListener { view ->
                connectToTvDevice(displayValue)
                dialog.cancel()

            }

            val cancel = dialog1.getButton(AlertDialog.BUTTON_NEGATIVE)
            cancel.setOnClickListener {
                dialog1.cancel()
            }
        }

        dialog.show()
    }

    private fun takePicture() {

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        try {
            imageFile = createImageFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val authorities: String = applicationContext.packageName + ".fileprovider"



        imageUri = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            Uri.fromFile(imageFile)
        } else {
            FileProvider.getUriForFile(this@SearchActivity, authorities, imageFile!!)
        }

        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, PHOTO_REQUEST)
        }

    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val storageDir = File(Environment.getExternalStorageDirectory(), "picture.jpg")
        if (!storageDir.exists()) {
            storageDir.parentFile.mkdirs()
            storageDir.createNewFile()
        }
        currImagePath = storageDir.absolutePath
        return storageDir
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (imageUri != null) {
            outState!!.putString(SAVED_INSTANCE_URI, imageUri!!.toString())
        }
        super.onSaveInstanceState(outState)
    }

    private fun launchMediaScanIntent(mediaScanIntent: Intent) {

        this.sendBroadcast(mediaScanIntent)
    }

    @Throws(FileNotFoundException::class)
    private fun decodeBitmapUri(ctx: Context, uri: Uri?): Bitmap? {

        val targetW = 600
        val targetH = 600
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true

        BitmapFactory.decodeStream(ctx.contentResolver.openInputStream(uri), null, bmOptions)
        val photoW = bmOptions.outWidth
        val photoH = bmOptions.outHeight

        val scaleFactor = Math.min(photoW / targetW, photoH / targetH)

        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor

        return BitmapFactory.decodeStream(
            ctx.contentResolver
                .openInputStream(uri), null, bmOptions
        )
    }

    //==================================================================================================================
    fun Context.restartApplication() {
        val intent = applicationContext.packageManager.getLaunchIntentForPackage(
            applicationContext.packageName
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    fun Context.changeLanguage(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = this.resources.configuration
        config.setLocale(locale)
        this.createConfigurationContext(config)
        this.resources.updateConfiguration(config, this.resources.displayMetrics)
    }

    fun saveLanguage(language: String) {
        GalaxyCastApplication.getPreferenceHelper().language = language
    }
}