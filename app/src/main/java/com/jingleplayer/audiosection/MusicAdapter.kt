package com.jingleplayer.audiosection

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jingleplayer.*
import com.jingleplayer.audiosection.AudioActivity.Companion.audioActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jingleplayer.audiosection.MusicAdapter.MyHolder
import com.jingleplayer.databinding.AudioMoreFeaturesBinding
import com.jingleplayer.databinding.DetailsViewBinding
import com.jingleplayer.databinding.MusicViewBinding
import com.jingleplayer.databinding.RenameFieldBinding
import com.google.android.material.color.MaterialColors
import java.io.File

class MusicAdapter(private val context: Context, private var musicList: ArrayList<Music>)
    : RecyclerView.Adapter<MyHolder>() {

    private var newPosition = 0
    private lateinit var dialogRF: AlertDialog

    class MyHolder(binding: MusicViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.songNameMV
        val album = binding.songAlbumMV
        val image = binding.imageMV
        val duration = binding.songDuration
        val size = binding.songSize
        val root = binding.root
        var fIndex: Int = -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(MusicViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: MyHolder, @SuppressLint("RecyclerView") position: Int) {
        holder.title.text = musicList[position].title
        holder.album.text = musicList[position].album
        holder.duration.text = formatDuration(musicList[position].duration)
        holder.size.text = Formatter.formatShortFileSize(context, musicList[position].size.toLong())
        Glide.with(context)
            .load(musicList[position].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_player_icon_slash_screen).centerCrop())
            .into(holder.image)

        //for play next feature
        holder.root.setOnLongClickListener {
            val location = IntArray(2)
            it.getLocationOnScreen(location)
            newPosition = position
            val customDialog = LayoutInflater.from(context).inflate(R.layout.audio_more_features, holder.root, false)
            val bindingMF = AudioMoreFeaturesBinding.bind(customDialog)
            val dialog = MaterialAlertDialogBuilder(context,R.style.alertDialog).setView(customDialog)
                .create()
            val lp = dialog.window?.attributes
            lp?.gravity = Gravity.TOP or Gravity.LEFT
            lp?.x = location[0]
            lp?.y = location[1]
            dialog.show()
            dialog.window?.setLayout(350,700)
            bindingMF.renameBtn.setOnClickListener {
                dialog.dismiss()
                requestWriteR()
            }

            bindingMF.shareBtn.setOnClickListener {
                dialog.dismiss()
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type = "audio/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(musicList[position].path))
                ContextCompat.startActivity(context, Intent.createChooser(shareIntent, "Sharing Audio File!!"), null)
            }

            bindingMF.infoBtn.setOnClickListener {
                dialog.dismiss()
                val detailsDialog = LayoutInflater.from(context).inflate(R.layout.details_view, bindingMF.root, false)
                val binder = DetailsViewBinding.bind(detailsDialog)
                binder.detailsTV.setTextColor(Color.BLACK)
                val dDialog = MaterialAlertDialogBuilder(context,R.style.alertDialog)
                    .setView(detailsDialog)
                    .setPositiveButton("OK"){self, _ -> self.dismiss()}
                    .setCancelable(false)
                    .create()
                dDialog.show()
                dDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
                val str = SpannableStringBuilder().bold { append("DETAILS\n\nName: ") }
                    .append(musicList[position].title)
                    .bold { append("\n\nDuration: ") }.append(DateUtils.formatElapsedTime(musicList[position].duration/1000))
                    .bold { append("\n\nLocation: ") }.append(musicList[position].path)
                    .bold { append("\n\nSize: ") }.append(Formatter.formatShortFileSize(context, musicList[position].size.toLong()))

                binder.detailsTV.text = str
            }

            bindingMF.deleteBtn.setOnClickListener {
                dialog.dismiss()
                requestDeleteR(position = position)
            }

            return@setOnLongClickListener true
        }

        holder.root.setOnClickListener {
            when{
                AudioActivity.search -> sendIntent(ref = "MusicAdapterSearch", pos = position)
                musicList[position].id == AudioPlayerActivity.nowPlayingId ->
                    sendIntent(ref = "NowPlaying", pos = AudioPlayerActivity.songPosition)
                else->sendIntent(ref="MusicAdapter", pos = position)
            }
        }
    }

    private fun requestDeleteR(position: Int){
        //list of audios to delete
        val uriList: List<Uri> = listOf(Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicList[position].id))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //requesting for delete permission
            val pi = MediaStore.createDeleteRequest(context.contentResolver, uriList)
            (context as Activity).startIntentSenderForResult(pi.intentSender, 123,
                null, 0, 0, 0, null)
        }
        else{
            //for devices less than android 11
            val file = File(musicList[position].path)
            val builder = MaterialAlertDialogBuilder(context,R.style.alertDialog)
            builder.setTitle("Delete Music?")
                .setMessage(musicList[position].title)
                .setPositiveButton("Yes"){ self, _ ->
                    if(file.exists() && file.delete()){
                        MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                        updateDeleteUI(position = position)
                    }
                    self.dismiss()
                }
                .setNegativeButton("No"){self, _ -> self.dismiss() }
            val delDialog = builder.create()
//            delDialog.window?.setBackgroundDrawable(ColorDrawable(R.color.white))
            delDialog.show()
            delDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(
                MaterialColors.getColor(context, R.attr.themeColor, Color.BLACK)
            )
            delDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(
                MaterialColors.getColor(context, R.attr.themeColor, Color.BLACK)
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateDeleteUI(position: Int){
        AudioActivity.MusicListMA = audioActivity.getAllAudio()
        musicList.removeAt(position)
        notifyDataSetChanged()
    }

    private fun requestWriteR(){
        //files to modify
        val uriList: List<Uri> = listOf(Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            musicList[newPosition].id))

        //requesting file write permission for specific files
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pi = MediaStore.createWriteRequest(context.contentResolver, uriList)
            (context as Activity).startIntentSenderForResult(pi.intentSender, 124,
                null, 0, 0, 0, null)
        }else renameFunction(newPosition)
    }

    private fun renameFunction(position: Int){
        val customDialogRF = LayoutInflater.from(context).inflate(R.layout.rename_field,
            (context as Activity).findViewById(R.id.clAudioActivity), false)
        val bindingRF = RenameFieldBinding.bind(customDialogRF)

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            dialogRF = MaterialAlertDialogBuilder(context,R.style.alertDialog).setView(customDialogRF)
                .setCancelable(false)
                .setPositiveButton("Rename"){ self, _ ->
                    val currentFile = File(musicList[position].path)
                    val newName = bindingRF.renameField.text
                    if(newName != null && currentFile.exists() && newName.toString().isNotEmpty()){
                        val newFile = File(currentFile.parentFile, newName.toString()+"."+currentFile.extension)

                        val fromUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            musicList[position].id)

                        ContentValues().also {
                            it.put(MediaStore.Files.FileColumns.IS_PENDING, 1)
                            context.contentResolver.update(fromUri, it, null, null)
                            it.clear()

                            //updating file details
                            it.put(MediaStore.Files.FileColumns.DISPLAY_NAME, newName.toString())
                            it.put(MediaStore.Files.FileColumns.IS_PENDING, 0)
                            context.contentResolver.update(fromUri, it, null, null)
                        }

                        updateRenameUI()
                    }
                    self.dismiss()
                }
                .setNegativeButton("Cancel"){self, _ ->
                    self.dismiss()
                }
                .create()
        }
        else{
            dialogRF = MaterialAlertDialogBuilder(context,R.style.alertDialog).setView(customDialogRF)
                .setCancelable(false)
                .setPositiveButton("Rename"){ self, _ ->
                    val currentFile = File(musicList[position].path)
                    val newName = bindingRF.renameField.text
                    if(newName != null && currentFile.exists() && newName.toString().isNotEmpty()){
                        val newFile = File(currentFile.parentFile, newName.toString()+"."+currentFile.extension)
                        if(currentFile.renameTo(newFile)){
                            MediaScannerConnection.scanFile(context, arrayOf(newFile.toString()), arrayOf("audio/*"), null)
                            updateRenameUI()
                        }
                    }
                    self.dismiss()
                }
                .setNegativeButton("Cancel"){self, _ ->
                    self.dismiss()
                }
                .create()
        }
        bindingRF.renameField.text = SpannableStringBuilder(musicList[newPosition].title)
        dialogRF.show()
        dialogRF.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
            MaterialColors.getColor(context,R.attr.themeColor, Color.BLACK))
        dialogRF.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
            MaterialColors.getColor(context,R.attr.themeColor, Color.BLACK))
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateRenameUI(){
        AudioActivity.MusicListMA = audioActivity.getAllAudio()
        musicList = AudioActivity.MusicListMA
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    fun updateMusicList(searchList : ArrayList<Music>){
        musicList = ArrayList()
        musicList.addAll(searchList)
        notifyDataSetChanged()
    }
    private fun sendIntent(ref: String, pos: Int){
        val intent = Intent(context, AudioPlayerActivity::class.java)
        intent.putExtra("index", pos)
        intent.putExtra("class", ref)
        ContextCompat.startActivity(context, intent, null)
        audioActivity.overridePendingTransition(R.anim.slide_in_top,R.anim.slide_out_bottom)
    }
}