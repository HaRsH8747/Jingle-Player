package com.jingleplayer.audiosection

import android.media.MediaMetadataRetriever
import com.jingleplayer.audiosection.favouritemusic.FavouriteActivity
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess


data class Music(val id:String, var title:String, val album:String, val artist:String, val duration: Long = 0, var path: String,
                 var artUri:String, val size:String,var displayName: String)

class Playlist{
    lateinit var name: String
    lateinit var playlist: ArrayList<Music>
    lateinit var createdBy: String
    lateinit var createdOn: String
}
class MusicPlaylist{
    var ref: ArrayList<Playlist> = ArrayList()
}

fun formatDuration(duration: Long):String{
    val minutes = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
    val seconds = (TimeUnit.SECONDS.convert(duration, TimeUnit.MILLISECONDS) -
            minutes*TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES))
    return String.format("%02d:%02d", minutes, seconds)
}
fun getImgArt(path: String): ByteArray? {
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(path)
    return retriever.embeddedPicture
}
fun setSongPosition(increment: Boolean){
    if(!AudioPlayerActivity.repeat){
        if(increment)
        {
            if(AudioPlayerActivity.musicListPA.size - 1 == AudioPlayerActivity.songPosition)
                AudioPlayerActivity.songPosition = 0
            else ++AudioPlayerActivity.songPosition
        }else{
            if(0 == AudioPlayerActivity.songPosition)
                AudioPlayerActivity.songPosition = AudioPlayerActivity.musicListPA.size-1
            else --AudioPlayerActivity.songPosition
        }
    }
}

fun exitApplication(){
    if(AudioPlayerActivity.musicService != null){
        AudioPlayerActivity.musicService!!.audioManager.abandonAudioFocus(AudioPlayerActivity.musicService)
        AudioPlayerActivity.musicService!!.stopForeground(true)
        AudioPlayerActivity.musicService!!.mediaPlayer!!.release()
        AudioPlayerActivity.musicService = null}
    exitProcess(1)
}

fun favouriteChecker(id: String): Int{
    AudioPlayerActivity.isFavourite = false
    FavouriteActivity.favouriteSongs.forEachIndexed { index, music ->
        if(id == music.id){
            AudioPlayerActivity.isFavourite = true
            return index
        }
    }
    return -1
}
fun checkPlaylist(playlist: ArrayList<Music>): ArrayList<Music>{
    playlist.forEachIndexed { index, music ->
        val file = File(music.path)
        if(!file.exists())
            playlist.removeAt(index)
    }
    return playlist
}