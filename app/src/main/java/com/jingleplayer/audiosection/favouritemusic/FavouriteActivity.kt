package com.jingleplayer.audiosection.favouritemusic

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.jingleplayer.audiosection.Music
import com.jingleplayer.R
import com.jingleplayer.audiosection.AudioPlayerActivity
import com.jingleplayer.audiosection.checkPlaylist
import com.jingleplayer.databinding.ActivityFavouriteBinding

class FavouriteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavouriteBinding
    private lateinit var adapter: FavouriteAdapter

    companion object{
        var favouriteSongs: ArrayList<Music> = ArrayList()
        var favouritesChanged: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_JinglePlayer)
        binding = ActivityFavouriteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        favouriteSongs = checkPlaylist(favouriteSongs)
        binding.backBtnFA.setOnClickListener { finish() }
        binding.favouriteRV.setHasFixedSize(true)
        binding.favouriteRV.setItemViewCacheSize(13)
        binding.favouriteRV.layoutManager = GridLayoutManager(this, 3)
        adapter = FavouriteAdapter(this, favouriteSongs)
        binding.favouriteRV.adapter = adapter

        binding.backBtnFA.setOnClickListener {
            onBackPressed()
        }
        favouritesChanged = false

        binding.shuffleBtnFA.setOnClickListener {
            val intent = Intent(this, AudioPlayerActivity::class.java)
            intent.putExtra("index", 0)
            intent.putExtra("class", "FavouriteShuffle")
            startActivity(intent)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        if(favouriteSongs.size < 1)
            binding.shuffleBtnFA.visibility = View.INVISIBLE
        if(favouriteSongs.isNotEmpty()){
            binding.instructionFV.visibility = View.GONE
        }else{
            binding.instructionFV.visibility = View.VISIBLE
        }
        if(favouritesChanged) {
            adapter.updateFavourites(favouriteSongs)
            favouritesChanged = false
        }
    }
}