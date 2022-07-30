package com.jingleplayer.audiosection

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jingleplayer.R
import com.jingleplayer.databinding.FragmentNowPlayingBinding

class NowPlaying : Fragment() {

    companion object{
        @SuppressLint("StaticFieldLeak")
        lateinit var binding: FragmentNowPlayingBinding
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_now_playing, container, false)
        binding = FragmentNowPlayingBinding.bind(view.rootView)
        binding.root.visibility = View.INVISIBLE
        binding.prevBtnNP.setOnClickListener {
            setSongPosition(increment = false)
            AudioPlayerActivity.musicService!!.createMediaPlayer()
            Glide.with(requireContext())
                .load(AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
                .into(binding.songImgNP)
            binding.songNameNP.text = AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].title
            AudioPlayerActivity.musicService!!.showNotification(R.drawable.pause_icon,true)
            playMusic()
        }
        binding.playPauseBtnNP.setOnClickListener {
            if(AudioPlayerActivity.isPlaying) pauseMusic() else playMusic()
        }
        binding.nextBtnNP.setOnClickListener {
            setSongPosition(increment = true)
            AudioPlayerActivity.musicService!!.createMediaPlayer()
            Glide.with(requireContext())
                .load(AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
                .into(binding.songImgNP)
            binding.songNameNP.text = AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].title
            AudioPlayerActivity.musicService!!.showNotification(R.drawable.pause_icon,true)
            playMusic()
        }
        binding.clMiniBar.setOnClickListener {
            val intent = Intent(requireContext(), AudioPlayerActivity::class.java)
            intent.putExtra("index", AudioPlayerActivity.songPosition)
            intent.putExtra("class", "NowPlaying")
            ContextCompat.startActivity(requireContext(), intent, null)
            requireActivity().overridePendingTransition(R.anim.slide_in_top,R.anim.slide_out_bottom)
        }
        return view.rootView
    }

    override fun onResume() {
        super.onResume()
        if(AudioPlayerActivity.musicService != null){
            binding.root.visibility = View.VISIBLE
            binding.songNameNP.isSelected = true
            Glide.with(requireContext())
                .load(AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].artUri)
                .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
                .into(binding.songImgNP)
            binding.songNameNP.text = AudioPlayerActivity.musicListPA[AudioPlayerActivity.songPosition].title
            if(AudioPlayerActivity.isPlaying) binding.playPauseBtnNP.setImageResource(R.drawable.pause_icon)
            else binding.playPauseBtnNP.setImageResource(R.drawable.play_icon)
        }
    }

    private fun playMusic(){
        AudioPlayerActivity.isPlaying = true
        AudioPlayerActivity.musicService!!.mediaPlayer!!.start()
        binding.playPauseBtnNP.setImageResource(R.drawable.pause_icon)
        AudioPlayerActivity.musicService!!.showNotification(R.drawable.pause_icon,true)
    }
    private fun pauseMusic(){
        AudioPlayerActivity.isPlaying = false
        AudioPlayerActivity.musicService!!.mediaPlayer!!.pause()
        binding.playPauseBtnNP.setImageResource(R.drawable.play_icon)
        AudioPlayerActivity.musicService!!.showNotification(R.drawable.play_icon,false)
    }
}