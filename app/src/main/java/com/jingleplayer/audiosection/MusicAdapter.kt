package com.jingleplayer.audiosection

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jingleplayer.*
import com.jingleplayer.audiosection.AudioActivity.Companion.audioActivity
import com.jingleplayer.audiosection.AudioActivity.Companion.renameIntentSenderLauncher
import com.jingleplayer.audiosection.MusicAdapter.MyHolder
import com.jingleplayer.databinding.AudioMoreFeaturesBinding
import com.jingleplayer.databinding.DetailsViewBinding
import com.jingleplayer.databinding.MusicViewBinding
import com.jingleplayer.databinding.RenameFieldBinding
import com.jingleplayer.utils.AppPref
import java.io.File
import java.util.regex.Pattern


class MusicAdapter(private val context: Context, private var musicList: ArrayList<Music>)
    : RecyclerView.Adapter<MyHolder>() {

    private var newPosition = 0
    private lateinit var dialogRF: AlertDialog
    private var renamePosition = 0
    private var finalNewName = ""
    private var appPref: AppPref = AppPref(context)

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
        holder.title.text = musicList[position].displayName
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
            val width = (180 * Resources.getSystem().displayMetrics.density + 0.5F).toInt()
            val height = (350 * Resources.getSystem().displayMetrics.density + 0.5F).toInt()
            dialog.window?.setLayout(width,height)
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
                val str = SpannableStringBuilder().bold { append("DETAILS\n\nTitle: ") }
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
                .setTitle("Delete Music?")
                .setMessage(musicList[position].displayName)
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
            delDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
            delDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateDeleteUI(position: Int){
        AudioActivity.MusicListMA = audioActivity.getAllAudio(false)
        musicList.removeAt(position)
        notifyDataSetChanged()
    }

    private fun requestWriteR(){
        //files to modify
        val uriList: List<Uri> = listOf(Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            musicList[newPosition].id))
        //requesting file write permission for specific files
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            val pi = MediaStore.createWriteRequest(context.contentResolver, uriList)
            val renameIntentSender = MediaStore.createWriteRequest(context.contentResolver, uriList)
            renameIntentSender.let {
                renameIntentSenderLauncher.launch(IntentSenderRequest.Builder(it).build())
            }
//            (context as Activity).startIntentSenderForResult(pi.intentSender, 124,
//                null, 0, 0, 0, null)
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
                    val newName = bindingRF.renameField.text.toString().replace(".${currentFile.extension}","")
                    val newFile = File(currentFile.parentFile, newName.toString()+"."+currentFile.extension)
                    val parentDir = File(currentFile.parentFile!!.toURI())

                    Log.d("CLEAR","parent: $parentDir")
                    if(currentFile.exists() && newName.toString().isNotEmpty()){
                        try {
                            val fromUri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                musicList[position].id)

                            ContentValues().also {
                                it.put(MediaStore.Audio.Media.IS_PENDING, 1)
                                context.contentResolver.update(fromUri, it, null, null)
                                it.clear()

                                //updating file details
                                it.put(MediaStore.Audio.Media.DISPLAY_NAME, newName.toString())
                                it.put(MediaStore.Audio.Media.IS_PENDING, 0)
                                context.contentResolver.update(fromUri, it, null, null)
                                updateRenameUI(position, newName = "$newName.${currentFile.extension}", newFile = newFile)
                            }
                        }catch (e: IllegalArgumentException){
                            e.printStackTrace()
                            try {
                                renamePosition = position
                                finalNewName = "$newName.${currentFile.extension}"
                                val permittedStorage = appPref.getString(AppPref.PERMITTED_AUDIO_STORAGE).toString()
                                val directories = permittedStorage.split("#@#")
                                for (directory in directories){
                                    if (directories.isNotEmpty()){
                                        val uri = directory.split("@#@")
                                        Log.d("CLEAR","uri: ${uri[0]} parent: $parentDir")
                                        if (uri[0] == parentDir.toString()){
                                            renameUsingSAF(Uri.parse(uri[1]))
                                            return@setPositiveButton
                                        }
                                    }
                                }
                                val builder =
                                    MaterialAlertDialogBuilder(context, R.style.alertDialog)
                                        .setTitle("Rename External Storage File")
                                        .setMessage("${musicList[position].title}\n\nTo Rename the Files from External SdCard Directory. Select the Directory in which the file is located")
                                        .setPositiveButton("Ok") { _, _ ->
                                            val intent =
                                                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                                                    // Optionally, specify a URI for the directory that should be opened in
                                                    // the system file picker when it loads.
                                                    putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                                                        parentDir)
                                                }
                                            context.startActivityForResult(intent, 23)
                                            self.dismiss()
                                        }
                                        .setNegativeButton("Cancel") { _, _ -> self.dismiss() }
                                val delDialog = builder.create()
                                delDialog.show()
                                delDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
                                    .setTextColor(Color.BLACK)
                                delDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE)
                                    .setTextColor(Color.BLACK)
//                                val manager: StorageManager = getSystemService(context,StorageManager::class.java)!!
//                                val primaryStorageVolume = manager.storageVolumes[1]
//                                context.startActivityForResult(primaryStorageVolume.createOpenDocumentTreeIntent(),23)
//                                val fromUri = Uri.withAppendedPath(MediaStore.Audio.Media.getContentUri(MediaStore.getExternalVolumeNames(context).toList()[1]),
//                                    musicList[position].id)
//                                ContentValues().also {
//                                    it.put(MediaStore.Audio.Media.IS_PENDING, 1)
//                                    context.contentResolver.update(fromUri, it, null, null)
//                                    it.clear()
//
//                                    //updating file details
//                                    it.put(MediaStore.Audio.Media.DISPLAY_NAME, newName.toString())
//                                    it.put(MediaStore.Audio.Media.IS_PENDING, 0)
//                                    context.contentResolver.update(fromUri, it, null, null)
//                                    updateRenameUI(position,
//                                        newName = "$newName.${currentFile.extension}",
//                                        newFile = newFile)
//                                }
//                                if(Environment.isExternalStorageManager()){
//
//                                }else{
//                                    val builder = MaterialAlertDialogBuilder(context,R.style.alertDialog)
//                                        .setTitle("Rename External Storage File")
//                                        .setMessage("${musicList[position].displayName}\n\nTo Rename the Files from External SdCard Directory. It requires All Files access from the App settings")
//                                        .setPositiveButton("Ok"){ _, _ ->
//                                            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//                                            intent.addCategory("android.intent.category.DEFAULT")
//                                            intent.data = Uri.parse("package:${context.applicationContext.packageName}")
//                                            ContextCompat.startActivity(context, intent, null)
//                                            self.dismiss()
//                                        }
//                                        .setNegativeButton("Cancel"){ _, _ -> self.dismiss() }
//                                    val delDialog = builder.create()
//                                    //            delDialog.window?.setBackgroundDrawable(ColorDrawable(R.color.white))
//                                    delDialog.show()
//                                    delDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK)
//                                    delDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK)
//                                }
                            }catch (e: Exception){
                                e.printStackTrace()
                            }
                        }
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
                    val parentDir = File(currentFile.parentFile!!.toURI())
                    Log.d("CLEAR","dir: $parentDir")
                    val newName = bindingRF.renameField.text.toString().replace(".${currentFile.extension}","")
                    try {
                        if(currentFile.exists() && newName.isNotEmpty()){
                            val newFile = File(currentFile.parentFile, newName+"."+currentFile.extension)
                            if(currentFile.renameTo(newFile)){
                                MediaScannerConnection.scanFile(context, arrayOf(newFile.toString()), arrayOf("audio/*"), null)
                                updateRenameUI(position, "$newName.${currentFile.extension}", newFile = newFile)
                            }
                        }
                    }catch (e: Exception){
                        Log.d("CLEAR","error ${e.printStackTrace()}")
                        e.printStackTrace()
                    }
                    self.dismiss()
                }
                .setNegativeButton("Cancel"){self, _ ->
                    self.dismiss()
                }
                .create()
        }
        bindingRF.renameField.text = SpannableStringBuilder(musicList[newPosition].displayName)
        dialogRF.show()
        dialogRF.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
            MaterialColors.getColor(context,R.attr.themeColor, Color.BLACK))
        dialogRF.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
            MaterialColors.getColor(context,R.attr.themeColor, Color.BLACK))
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateRenameUI(position: Int, newName: String, newFile: File){
        AudioActivity.MusicListMA[position].displayName = newName
        AudioActivity.MusicListMA[position].path = newFile.path
//        AudioActivity.MusicListMA[position].artUri = Uri.fromFile(newFile).toString()
        notifyItemChanged(position)
//        AudioActivity.MusicListMA = audioActivity.getAllAudio()
//        musicList = AudioActivity.MusicListMA
//        notifyDataSetChanged()
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

    fun onResult(requestCode: Int, resultCode: Int, data: Intent?){
        if(resultCode == Activity.RESULT_OK && requestCode == 123) updateDeleteUI(newPosition)
        if(resultCode == Activity.RESULT_OK && requestCode == 23) {
            val treeUri = data!!.data!!
            context.contentResolver.takePersistableUriPermission(treeUri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val currentFile = File(musicList[renamePosition].path)
            val parentDir = File(currentFile.parentFile!!.toURI())
            var permittedStorage = appPref.getString(AppPref.PERMITTED_AUDIO_STORAGE).toString()
            permittedStorage += "$parentDir@#@$treeUri#@#"
            Log.d("CLEAR","treeUri|| $treeUri")
            appPref.setString(AppPref.PERMITTED_AUDIO_STORAGE,permittedStorage)
            renameUsingSAF(treeUri)
        }
    }

    private fun renameUsingSAF(treeUri: Uri){
        val currentFile = File(musicList[renamePosition].path)
        val fileDoc = DocumentFile.fromTreeUri(context,treeUri)
        for (file in fileDoc!!.listFiles()){
            if (file.name.equals(currentFile.name)){
                if (file.renameTo(finalNewName)){
                    updateRenameUI(renamePosition, newName = finalNewName, newFile = File(file.uri.path!!))
                }else{
                    Log.d("CLEAR","not")
                }
            }
        }
    }

    fun renameResult(){
        renameFunction(newPosition)
    }

}