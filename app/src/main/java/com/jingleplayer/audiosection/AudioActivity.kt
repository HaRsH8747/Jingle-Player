package com.jingleplayer.audiosection

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jingleplayer.*
import com.jingleplayer.audiosection.favouritemusic.FavouriteActivity
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.jingleplayer.databinding.ActivityAudioBinding
import java.io.File

class AudioActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioBinding
    private lateinit var musicAdapter: MusicAdapter
    private var spinnerIndex = 0
    private val searchType =
        arrayOf("Recent", "Oldest", "Name(A to Z)", "Name(Z to A)", "File Size(Smallest)", "File Size(Largest)")

    companion object{
        lateinit var MusicListMA : ArrayList<Music>
        lateinit var musicListSearch : ArrayList<Music>
        lateinit var audioActivity: AudioActivity
        var search: Boolean = false
        var sortOrder: Int = 0
        val sortingList = arrayOf(
            MediaStore.Video.Media.DATE_ADDED + " DESC",
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.TITLE + " DESC",
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.SIZE + " DESC"
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_JinglePlayer)
        binding = ActivityAudioBinding.inflate(layoutInflater)
        setContentView(binding.root)
        audioActivity = this
        val adapter: ArrayAdapter<String> =
            ArrayAdapter<String>(this, R.layout.spinner_item, searchType)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = adapter
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
                spinnerIndex = i
                if(sortOrder != spinnerIndex){
                    sortOrder = spinnerIndex
                    MusicListMA = getAllAudio()
                    musicAdapter.updateMusicList(MusicListMA)
                }
            }
            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        binding.back.setOnClickListener {onBackPressed()}
        binding.ivFavourites.setOnClickListener {
            val intent = Intent(this,FavouriteActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
        }

        binding.search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                binding.search.removeTextChangedListener(this)
                if(binding.search.text?.isNotEmpty() == true){
                    musicListSearch = ArrayList()
                    val userInput = binding.search.text.toString().lowercase()
                    for (song in MusicListMA)
                        if(song.title.lowercase().contains(userInput))
                            musicListSearch.add(song)
                    search = true
                    musicAdapter.updateMusicList(searchList = musicListSearch)
                }else{
                    musicAdapter.updateMusicList(MusicListMA)
                }
                binding.search.addTextChangedListener(this)
            }
        })

        if(requestRuntimePermission()){
            initializeLayout()
            FavouriteActivity.favouriteSongs = ArrayList()
            val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE)
            val jsonString = editor.getString("FavouriteSongs", null)
            val typeToken = object : TypeToken<ArrayList<Music>>(){}.type
            if(jsonString != null){
                val data: ArrayList<Music> = GsonBuilder().create().fromJson(jsonString, typeToken)
                FavouriteActivity.favouriteSongs.addAll(data)
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun requestRuntimePermission() :Boolean{
        if(ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 13)
            return false
        }
        return true
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 13){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission Granted",Toast.LENGTH_SHORT).show()
                initializeLayout()
            }
            else
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 13)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @SuppressLint("SetTextI18n")
    private fun initializeLayout(){
        search = false
        val sortEditor = getSharedPreferences("SORTING", MODE_PRIVATE)
        sortOrder = sortEditor.getInt("sortOrder", 0)
        MusicListMA = getAllAudio()
        binding.musicRV.setHasFixedSize(true)
        binding.musicRV.setItemViewCacheSize(13)
        binding.musicRV.layoutManager = LinearLayoutManager(this)
        musicAdapter = MusicAdapter(this, MusicListMA)
        binding.musicRV.adapter = musicAdapter

        //for refreshing layout on swipe from top
        binding.refreshLayout.setOnRefreshListener {
            if (binding.search.text.isNotEmpty()){
                musicAdapter.updateMusicList(searchList = musicListSearch)
            }else{
                MusicListMA = getAllAudio()
            }
//            musicAdapter.updateMusicList(MusicListMA)
            binding.refreshLayout.isRefreshing = false
        }
    }
    @SuppressLint("Recycle", "Range")
    @RequiresApi(Build.VERSION_CODES.R)
    fun getAllAudio(): ArrayList<Music>{
        val tempList = ArrayList<Music>()
        val selection = MediaStore.Audio.Media.IS_MUSIC +  " != 0"
        val projection = arrayOf(MediaStore.Audio.Media._ID,MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.DURATION,MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ALBUM_ID, MediaStore.Audio.Media.SIZE)
        val cursor = this.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,selection,null,
        sortingList[sortOrder], null)
        if(cursor != null){
            if(cursor.moveToFirst())
                do {
                    val titleC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))?:"Unknown"
                    val idC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID))?:"Unknown"
                    val albumC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM))?:"Unknown"
                    val artistC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST))?:"Unknown"
                    val pathC = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                    val durationC = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION))
                    val albumIdC = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)).toString()
                    val size = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE))
                    val uri = Uri.parse("content://media/external/audio/albumart")
                    val artUriC = Uri.withAppendedPath(uri, albumIdC).toString()
                    val music = Music(id = idC, title = titleC, album = albumC, artist = artistC, path = pathC, duration = durationC,
                    artUri = artUriC, size = size)
                    val file = File(music.path)
                    if(file.exists())
                        tempList.add(music)
                }while (cursor.moveToNext())
                cursor.close()
        }
        return tempList
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!AudioPlayerActivity.isPlaying){
            JingleMainClass.notificationManager.cancelAll()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onResume() {
        super.onResume()
        val editor = getSharedPreferences("FAVOURITES", MODE_PRIVATE).edit()
        val jsonString = GsonBuilder().create().toJson(FavouriteActivity.favouriteSongs)
        editor.putString("FavouriteSongs", jsonString)
        editor.apply()
        val sortEditor = getSharedPreferences("SORTING", MODE_PRIVATE)
        val sortValue = sortEditor.getInt("sortOrder", 0)
        if(sortOrder != sortValue){
            sortOrder = sortValue
            MusicListMA = getAllAudio()
            musicAdapter.updateMusicList(MusicListMA)
        }
        if(AudioPlayerActivity.musicService != null) binding.nowPlaying.visibility = View.VISIBLE
    }
}