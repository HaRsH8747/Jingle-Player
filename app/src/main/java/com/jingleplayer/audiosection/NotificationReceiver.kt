package com.jingleplayer.audiosection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jingleplayer.*

class NotificationReceiver:BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action){
            JingleMainClass.PREVIOUS -> prevNextSong(increment = false, context = context!!)
            JingleMainClass.PLAY -> if(AudioPlayerActivity.isPlaying) pauseMusic() else playMusic()
            JingleMainClass.NEXT -> prevNextSong(increment = true, context = context!!)
            JingleMainClass.EXIT ->{
                exitApplication()
            }
        }
    }
    private fun playMusic(){
        AudioPlayerActivity.isPlaying = true
        AudioPlayerActivity.musicService!!.mediaPlayer!!.start()
        AudioPlayerActivity.musicService!!.showNotification(R.drawable.pause_icon,true)
        AudioPlayerActivity.binding.playPauseBtnPA.setImageResource(R.drawable.pause_icon)
        NowPlaying.binding.playPauseBtnNP.setImageResource(R.drawable.pause_icon)
    }

    private fun pauseMusic(){
        AudioPlayerActivity.isPlaying = false
        AudioPlayerActivity.musicService!!.mediaPlayer!!.pause()
        AudioPlayerActivity.musicService!!.showNotification(R.drawable.play_icon,false)
        AudioPlayerActivity.binding.playPauseBtnPA.setImageResource(R.drawable.play_icon)
        NowPlaying.binding.playPauseBtnNP.setImageResource(R.drawable.play_icon)
    }

    private fun prevNextSong(increment: Boolean, context: Context){
        setSongPosition(increment = increment)
        AudioPlayerActivity.musicService!!.createMediaPlayer()
        Glide.with(context)
            .load(AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
            .into(AudioPlayerActivity.binding.songImgPA)
        AudioPlayerActivity.binding.songNamePA.text = AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].title
        Glide.with(context)
            .load(AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
            .into(NowPlaying.binding.songImgNP)
        NowPlaying.binding.songNameNP.text = AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].title
        playMusic()
        AudioPlayerActivity.fIndex = favouriteChecker(AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].id)
        if(AudioPlayerActivity.isFavourite) AudioPlayerActivity.binding.favouriteBtnPA.setImageResource(R.drawable.favourite_icon)
        else AudioPlayerActivity.binding.favouriteBtnPA.setImageResource(R.drawable.favourite_empty_icon)
    }
}