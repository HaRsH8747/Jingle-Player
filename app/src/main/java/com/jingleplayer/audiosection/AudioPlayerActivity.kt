package com.jingleplayer.audiosection

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.database.Cursor
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jingleplayer.*
import com.jingleplayer.audiosection.favouritemusic.FavouriteActivity
import com.jingleplayer.databinding.ActivityAudioPlayerBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.color.MaterialColors

class AudioPlayerActivity : AppCompatActivity(), ServiceConnection, MediaPlayer.OnCompletionListener {

    companion object {
        lateinit var musicListPA : ArrayList<Music>
        var songPosition: Int = 0
        var isPlaying:Boolean = false
        var musicService: MusicService? = null
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: ActivityAudioPlayerBinding
        var repeat: Boolean = false
        var min15: Boolean = false
        var min30: Boolean = false
        var min60: Boolean = false
        var nowPlayingId: String = ""
        var isFavourite: Boolean = false
        var fIndex: Int = -1
        lateinit var loudnessEnhancer: LoudnessEnhancer
        var isMuted: Boolean = false
    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_JinglePlayer)
        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if(intent.data?.scheme.contentEquals("content")){
            val intentService = Intent(this, MusicService::class.java)
            bindService(intentService, this, BIND_AUTO_CREATE)
            startService(intentService)
            musicListPA = ArrayList()
            musicListPA.add(getMusicDetails(intent.data!!))
            Glide.with(this)
                .load(getImgArt(musicListPA[songPosition].path))
                .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
                .into(binding.songImgPA)
            binding.songNamePA.text = musicListPA[songPosition].displayName

        }
        else initializeLayout()

        binding.muteBtnPA.setOnClickListener {
            isMuted = if (!isMuted){
                binding.muteBtnPA.setImageResource(R.drawable.ic_mute)
                musicService?.mediaPlayer?.setVolume(0F,0F)
                true
            }else{
                binding.muteBtnPA.setImageResource(R.drawable.volume_icon)
                musicService?.mediaPlayer?.setVolume(1F,1F)
                false
            }
        }

        binding.backBtnPA.setOnClickListener {
            onBackPressed()
        }
        binding.playPauseBtnPA.setOnClickListener{ if(isPlaying) pauseMusic() else playMusic() }
        binding.previousBtnPA.setOnClickListener { prevNextSong(increment = false) }
        binding.nextBtnPA.setOnClickListener { prevNextSong(increment = true) }
        binding.seekBarPA.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if(fromUser) {
                    musicService!!.mediaPlayer!!.seekTo(progress)
                    musicService!!.showNotification(if(isPlaying) R.drawable.pause_icon else R.drawable.play_icon,isPlaying)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        binding.repeatBtnPA.setOnClickListener {
            if(!repeat){
                repeat = true
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.colorOnSecondary))
            }else{
                repeat = false
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_pink))
            }
        }
        binding.equalizerBtnPA.setOnClickListener {
        try {
            val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            eqIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, musicService!!.mediaPlayer!!.audioSessionId)
            eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, baseContext.packageName)
            eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            startActivityForResult(eqIntent, 13)
        }catch (e: Exception){Toast.makeText(this,  "Equalizer Feature not Supported!!", Toast.LENGTH_SHORT).show()}
        }
        binding.timerBtnPA.setOnClickListener {
            val timer = min15 || min30 || min60
            if(!timer) showBottomSheetDialog()
            else {
                val builder = MaterialAlertDialogBuilder(this,R.style.alertDialog)
                    builder.setTitle("Stop Timer")
                        .setMessage("Do you want to stop timer?")
                        .setPositiveButton("Yes"){ _, _ ->
                            min15 = false
                            min30 = false
                            min60 = false
                            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this,
                                R.color.cool_pink))
                        }
                        .setNegativeButton("No"){dialog, _ ->
                            dialog.dismiss()
                        }
                    val customDialog = builder.create()
                    customDialog.show()
                customDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(
                    MaterialColors.getColor(this, R.attr.themeColor, Color.BLACK)
                )
                customDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(
                    MaterialColors.getColor(this, R.attr.themeColor, Color.BLACK)
                )
                }
        }
        binding.shareBtnPA.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "audio/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicListPA[songPosition].path))
            startActivity(Intent.createChooser(shareIntent, "Sharing Music File!!"))
        }
        binding.favouriteBtnPA.setOnClickListener {
            fIndex = favouriteChecker(musicListPA[songPosition].id)
            if(isFavourite){
                isFavourite = false
                binding.favouriteBtnPA.setImageResource(R.drawable.favourite_empty_icon)
                FavouriteActivity.favouriteSongs.removeAt(fIndex)
            } else{
                isFavourite = true
                binding.favouriteBtnPA.setImageResource(R.drawable.favourite_icon)
                FavouriteActivity.favouriteSongs.add(musicListPA[songPosition])
            }
            FavouriteActivity.favouritesChanged = true
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_bottom,R.anim.slide_out_top)
    }

    override fun onResume() {
        super.onResume()
        binding.songNamePA.isSelected = true
        if (isMuted){
            binding.muteBtnPA.setImageResource(R.drawable.ic_mute)
        }else{
            binding.muteBtnPA.setImageResource(R.drawable.volume_icon)
        }
    }

    private fun initializeLayout(){
        songPosition = intent.getIntExtra("index", 0)
        when(intent.getStringExtra("class")){
            "NowPlaying"->{
                setLayout()
                binding.tvSeekBarStart.text = formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                binding.tvSeekBarEnd.text = formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
                binding.seekBarPA.progress = musicService!!.mediaPlayer!!.currentPosition
                binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
                if(isPlaying) binding.playPauseBtnPA.setImageResource(R.drawable.pause_icon)
                else binding.playPauseBtnPA.setImageResource(R.drawable.play_icon)
            }
            "MusicAdapterSearch"-> initServiceAndPlaylist(AudioActivity.musicListSearch, shuffle = false)
            "MusicAdapter" -> initServiceAndPlaylist(AudioActivity.MusicListMA, shuffle = false)
            "FavouriteAdapter"-> initServiceAndPlaylist(FavouriteActivity.favouriteSongs, shuffle = false)
            "MainActivity"-> initServiceAndPlaylist(AudioActivity.MusicListMA, shuffle = true)
            "FavouriteShuffle"-> initServiceAndPlaylist(FavouriteActivity.favouriteSongs, shuffle = true)
        }
    if (musicService != null && !isPlaying) playMusic()
    }

    private fun setLayout(){
        fIndex = favouriteChecker(musicListPA[songPosition].id)
        Glide.with(applicationContext)
            .load(musicListPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
            .into(binding.songImgPA)
        binding.songNamePA.text = musicListPA[songPosition].displayName
        if(repeat) binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
            R.color.colorOnSecondary))
        if(min15 || min30 || min60) binding.timerBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
            R.color.colorOnSecondary))
        if(isFavourite) binding.favouriteBtnPA.setImageResource(R.drawable.favourite_icon)
        else binding.favouriteBtnPA.setImageResource(R.drawable.favourite_empty_icon)

    }

    private fun createMediaPlayer(){
        try {
            if (musicService!!.mediaPlayer == null) musicService!!.mediaPlayer = MediaPlayer()
            musicService!!.mediaPlayer!!.reset()
            musicService!!.mediaPlayer!!.setDataSource(musicListPA[songPosition].path)
            musicService!!.mediaPlayer!!.prepare()
            binding.tvSeekBarStart.text = formatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
            binding.tvSeekBarEnd.text = formatDuration(musicService!!.mediaPlayer!!.duration.toLong())
            binding.seekBarPA.progress = 0
            binding.seekBarPA.max = musicService!!.mediaPlayer!!.duration
            musicService!!.mediaPlayer!!.setOnCompletionListener(this)
            nowPlayingId = musicListPA[songPosition].id
            playMusic()
            loudnessEnhancer = LoudnessEnhancer(musicService!!.mediaPlayer!!.audioSessionId)
            loudnessEnhancer.enabled = true
        }catch (e: Exception){Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()}
    }

    private fun playMusic(){
        isPlaying = true
        musicService!!.mediaPlayer!!.start()
        binding.playPauseBtnPA.setImageResource(R.drawable.pause_icon)
        musicService!!.showNotification(R.drawable.pause_icon, isPlaying)
    }

    private fun pauseMusic(){
        isPlaying = false
        musicService!!.mediaPlayer!!.pause()
        binding.playPauseBtnPA.setImageResource(R.drawable.play_icon)
        musicService!!.showNotification(R.drawable.play_icon,isPlaying)
    }
    private fun prevNextSong(increment: Boolean){
        if(increment)
        {
            setSongPosition(increment = true)
            setLayout()
            createMediaPlayer()
        }
        else{
            setSongPosition(increment = false)
            setLayout()
            createMediaPlayer()
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        if(musicService == null){
            val binder = service as MusicService.MyBinder
            musicService = binder.currentService()
            musicService!!.audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            musicService!!.audioManager.requestAudioFocus(musicService, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            }
        createMediaPlayer()
        musicService!!.seekBarSetup()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        musicService = null
    }

    override fun onCompletion(mp: MediaPlayer?) {
        setSongPosition(increment = true)
        createMediaPlayer()
        setLayout()

        NowPlaying.binding.songNameNP.isSelected = true
        Glide.with(applicationContext)
            .load(musicListPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
            .into(NowPlaying.binding.songImgNP)
        NowPlaying.binding.songNameNP.text = musicListPA[songPosition].displayName
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 13 || resultCode == RESULT_OK)
            return
    }

    private fun showBottomSheetDialog(){
        val dialog = BottomSheetDialog(this@AudioPlayerActivity)
        dialog.setContentView(R.layout.bottom_sheet_dialog)
        dialog.show()
        dialog.findViewById<LinearLayout>(R.id.min_15)?.setOnClickListener {
            Toast.makeText(baseContext,  "Music will stop after 15 minutes", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.colorOnSecondary))
            min15 = true
            Thread{Thread.sleep((15 * 60000).toLong())
            if(min15) exitApplication()
            }.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_30)?.setOnClickListener {
            Toast.makeText(baseContext,  "Music will stop after 30 minutes", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.colorOnSecondary))
            min30 = true
            Thread{Thread.sleep((30 * 60000).toLong())
                if(min30) exitApplication()
            }.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_60)?.setOnClickListener {
            Toast.makeText(baseContext,  "Music will stop after 60 minutes", Toast.LENGTH_SHORT).show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.colorOnSecondary))
            min60 = true
            Thread{Thread.sleep((60 * 60000).toLong())
                if(min60) exitApplication()
            }.start()
            dialog.dismiss()
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getMusicDetails(contentUri: Uri): Music {
        var cursor: Cursor? = null
        try {
            val projection = arrayOf(MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION,MediaStore.Audio.Media.SIZE, MediaStore.Audio.Media.DISPLAY_NAME)
            cursor = this.contentResolver.query(contentUri, projection, null, null, null)
            val dataColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val displayColumn = cursor?.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            cursor!!.moveToFirst()
            val path = dataColumn?.let { cursor.getString(it) }
            val duration = durationColumn?.let { cursor.getLong(it) }!!
            val size = sizeColumn?.let { cursor.getString(it) }!!
            val displayName = displayColumn?.let { cursor.getString(it) }!!
            return Music(id = "Unknown", title = path.toString(), album = "Unknown", artist = "Unknown", duration = duration,
            artUri = "Unknown", path = path.toString(), size = size, displayName = displayName)
        }finally {
            cursor?.close()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(musicListPA[songPosition].id == "Unknown" && !isPlaying) exitApplication()
    }
    private fun initServiceAndPlaylist(playlist: ArrayList<Music>, shuffle: Boolean, playNext: Boolean = false){
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, this, BIND_AUTO_CREATE)
        startService(intent)
        musicListPA = ArrayList()
        musicListPA.addAll(playlist)
        if(shuffle) musicListPA.shuffle()
        setLayout()
    }
}