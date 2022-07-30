package com.jingleplayer.audiosection

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.os.*
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.jingleplayer.*

class MusicService: Service(), AudioManager.OnAudioFocusChangeListener {
    private var myBinder = MyBinder()
    var mediaPlayer:MediaPlayer? = null
    private lateinit var mediaSession : MediaSessionCompat
    private lateinit var runnable: Runnable
    lateinit var audioManager: AudioManager

    override fun onBind(intent: Intent?): IBinder {
        mediaSession = MediaSessionCompat(baseContext, "My Music")
        return myBinder
    }

    inner class MyBinder:Binder(){
        fun currentService(): MusicService {
            return this@MusicService
        }
    }
    @SuppressLint("UnspecifiedImmutableFlag")
    fun showNotification(playPauseBtn: Int,onGoing: Boolean){
        val intent = Intent(baseContext, AudioActivity::class.java)
        val contentIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val prevIntent = Intent(baseContext, NotificationReceiver::class.java).setAction(
            JingleMainClass.PREVIOUS)
        val prevPendingIntent = PendingIntent.getBroadcast(baseContext, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val playIntent = Intent(baseContext, NotificationReceiver::class.java).setAction(
            JingleMainClass.PLAY)
        val playPendingIntent = PendingIntent.getBroadcast(baseContext, 0, playIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val nextIntent = Intent(baseContext, NotificationReceiver::class.java).setAction(
            JingleMainClass.NEXT)
        val nextPendingIntent = PendingIntent.getBroadcast(baseContext, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val exitIntent = Intent(baseContext, NotificationReceiver::class.java).setAction(
            JingleMainClass.EXIT)
        val exitPendingIntent = PendingIntent.getBroadcast(baseContext, 0, exitIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val imgArt = getImgArt(AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].path)
        val image = if(imgArt != null){
            BitmapFactory.decodeByteArray(imgArt, 0, imgArt.size)
        }else{
            BitmapFactory.decodeResource(resources, R.drawable.music_player_icon_slash_screen)
        }

        val notification: Notification = androidx.core.app.NotificationCompat.Builder(baseContext,
            JingleMainClass.CHANNEL_ID)
            .setContentIntent(contentIntent)
            .setContentTitle(AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].title)
            .setContentText(AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].artist)
            .setSmallIcon(R.drawable.music_icon)
            .setLargeIcon(image)
            .setOngoing(true)
            .setShowWhen(false)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.backward_icon, "Previous", prevPendingIntent)
            .addAction(playPauseBtn, "Play", playPendingIntent)
            .addAction(R.drawable.a_forward_icon, "Next", nextPendingIntent)
            .addAction(R.drawable.exit_icon, "Exit", exitPendingIntent)
            .build()
//        if (onGoing){
//            Log.d("CLEAR","onGoing $onGoing")
//        }else{
//            Log.d("CLEAR","onGoing $onGoing")
//            notification = androidx.core.app.NotificationCompat.Builder(baseContext,
//                JingleMainClass.CHANNEL_ID)
//                .setContentIntent(contentIntent)
//                .setContentTitle(AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].title)
//                .setContentText(AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].artist)
//                .setSmallIcon(R.drawable.music_icon)
//                .setLargeIcon(image)
//                .setOngoing(false)
//                .setShowWhen(false)
//                .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))
//                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
//                .setOnlyAlertOnce(true)
//                .addAction(R.drawable.backward_icon, "Previous", prevPendingIntent)
//                .addAction(playPauseBtn, "Play", playPendingIntent)
//                .addAction(R.drawable.a_forward_icon, "Next", nextPendingIntent)
//                .addAction(R.drawable.exit_icon, "Exit", exitPendingIntent)
//                .build()
//        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            val playbackSpeed = if(AudioPlayerActivity.isPlaying) 1F else 0F
            mediaSession.setMetadata(MediaMetadataCompat.Builder()
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer!!.duration.toLong())
                .build())
            val playBackState = PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer!!.currentPosition.toLong(), playbackSpeed)
                .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                .build()
            mediaSession.setPlaybackState(playBackState)
            mediaSession.setCallback(object: MediaSessionCompat.Callback(){
                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    mediaPlayer!!.seekTo(pos.toInt())
                    val playBackStateNew = PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, mediaPlayer!!.currentPosition.toLong(), playbackSpeed)
                        .setActions(PlaybackStateCompat.ACTION_SEEK_TO)
                        .build()
                    mediaSession.setPlaybackState(playBackStateNew)
                }
            })
        }

        startForeground(13, notification)
    }
    fun createMediaPlayer(){
        try {
            if (mediaPlayer == null) mediaPlayer = MediaPlayer()
            mediaPlayer!!.reset()
            mediaPlayer!!.setDataSource(AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].path)
            mediaPlayer!!.prepare()
            AudioPlayerActivity.binding.playPauseBtnPA.setImageResource(R.drawable.pause_icon)
            showNotification(R.drawable.pause_icon,true)
            AudioPlayerActivity.binding.tvSeekBarStart.text = formatDuration(mediaPlayer!!.currentPosition.toLong())
            AudioPlayerActivity.binding.tvSeekBarEnd.text = formatDuration(mediaPlayer!!.duration.toLong())
            AudioPlayerActivity.binding.seekBarPA.progress = 0
            AudioPlayerActivity.binding.seekBarPA.max = mediaPlayer!!.duration
            AudioPlayerActivity.nowPlayingId = AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].id
            AudioPlayerActivity.loudnessEnhancer = LoudnessEnhancer(mediaPlayer!!.audioSessionId)
            AudioPlayerActivity.loudnessEnhancer.enabled = true
        }catch (e: Exception){return}
    }

    fun seekBarSetup(){
        runnable = Runnable {
            AudioPlayerActivity.binding.tvSeekBarStart.text = formatDuration(mediaPlayer!!.currentPosition.toLong())
            AudioPlayerActivity.binding.seekBarPA.progress = mediaPlayer!!.currentPosition
            Handler(Looper.getMainLooper()).postDelayed(runnable, 200)
        }
        Handler(Looper.getMainLooper()).postDelayed(runnable, 0)
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if(focusChange <= 0){
            //pause music
            AudioPlayerActivity.binding.playPauseBtnPA.setImageResource(R.drawable.play_icon)
            NowPlaying.binding.playPauseBtnNP.setImageResource(R.drawable.play_icon)
            AudioPlayerActivity.isPlaying = false
            mediaPlayer!!.pause()
            showNotification(R.drawable.play_icon,false)
        }
//        else{
//            //play music
//            PlayerActivity.binding.playPauseBtnPA.setIconResource(R.drawable.pause_icon)
//            NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.pause_icon)
//            PlayerActivity.isPlaying = true
//            mediaPlayer!!.start()
//            showNotification(R.drawable.pause_icon)
//        }
    }

    //for making persistent
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}