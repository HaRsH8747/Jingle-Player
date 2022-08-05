package com.jingleplayer.videosection

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.core.text.bold
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jingleplayer.R
import com.jingleplayer.databinding.*
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jingleplayer.utils.AppPref
import java.io.File
import java.util.regex.Pattern

class VideoAdapter(
    private val context: Context,
    private var videoList: ArrayList<Video>,
    private var isFolder: Boolean = false,
) : RecyclerView.Adapter<VideoAdapter.MyHolder>() {

    private var newPosition = 0
    private lateinit var dialogRF: androidx.appcompat.app.AlertDialog
    private var renamePosition = 0
    private var finalNewName = ""
    private var appPref: AppPref = AppPref(context)

    class MyHolder(binding: VideoViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.videoName
        val folder = binding.folderName
        val duration = binding.duration
        val size = binding.size
        val image = binding.videoImg
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(VideoViewBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyHolder, @SuppressLint("RecyclerView") position: Int) {
        holder.title.text = videoList[position].title
        holder.folder.text = videoList[position].folderName
        holder.duration.text = DateUtils.formatElapsedTime(videoList[position].duration / 1000)
        holder.size.text = Formatter.formatShortFileSize(context, videoList[position].size.toLong())
        Glide.with(context)
            .asBitmap()
            .load(videoList[position].artUri)
            .apply(RequestOptions().placeholder(R.mipmap.ic_jingle_player_launcher).centerCrop())
            .into(holder.image)
        holder.root.setOnClickListener {
            when {
                videoList[position].id == VideoPlayerActivity.nowPlayingId -> {
                    sendIntent(pos = position, ref = "NowPlaying")
                }
                isFolder -> {
                    VideoPlayerActivity.pipStatus = 1
                    sendIntent(pos = position, ref = "FolderActivity")
                }
                VideoActivity.search -> {
                    VideoPlayerActivity.pipStatus = 2
                    sendIntent(pos = position, ref = "SearchedVideos")
                }
                else -> {
                    VideoPlayerActivity.pipStatus = 3
                    sendIntent(pos = position, ref = "AllVideos")
                }
            }
        }
        holder.root.setOnLongClickListener {
            val location = IntArray(2)
            it.getLocationOnScreen(location)
            newPosition = position
            val customDialog = LayoutInflater.from(context)
                .inflate(R.layout.video_more_features2, holder.root, false)
            val bindingMF = VideoMoreFeatures2Binding.bind(customDialog)
            val dialog =
                MaterialAlertDialogBuilder(context, R.style.alertDialog).setView(customDialog)
                    .create()
            val lp = dialog.window?.attributes
            lp?.gravity = Gravity.TOP or Gravity.LEFT
            lp?.x = location[0]
            lp?.y = location[1]
            dialog.show()
            val width = (180 * Resources.getSystem().displayMetrics.density + 0.5F).toInt()
            val height = (350 * Resources.getSystem().displayMetrics.density + 0.5F).toInt()
            dialog.window?.setLayout(width, height)
            bindingMF.renameBtn.setOnClickListener {
                dialog.dismiss()
                requestWriteR()
            }

            bindingMF.shareBtn.setOnClickListener {
                dialog.dismiss()
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.type = "video/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(videoList[position].path))
                ContextCompat.startActivity(context,
                    Intent.createChooser(shareIntent, "Sharing Video File!!"),
                    null)
            }

            bindingMF.infoBtn.setOnClickListener {
                dialog.dismiss()
                val customDialogIF =
                    LayoutInflater.from(context).inflate(R.layout.details_view, holder.root, false)
                val bindingIF = DetailsViewBinding.bind(customDialogIF)
                val dialogIF =
                    MaterialAlertDialogBuilder(context, R.style.alertDialog).setView(customDialogIF)
                        .setCancelable(false)
                        .setPositiveButton("Ok") { self, _ ->
                            self.dismiss()
                        }
                        .create()
                dialogIF.show()
                val infoText = SpannableStringBuilder().bold { append("DETAILS\n\nName: ") }
                    .append(videoList[position].title)
                    .bold { append("\n\nDuration: ") }
                    .append(DateUtils.formatElapsedTime(videoList[position].duration / 1000))
                    .bold { append("\n\nFile Size: ") }
                    .append(Formatter.formatShortFileSize(context,
                        videoList[position].size.toLong()))
                    .bold { append("\n\nLocation: ") }.append(videoList[position].path)

                bindingIF.detailsTV.text = infoText
                dialogIF.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                    setTextColor(MaterialColors.getColor(context, R.attr.themeColor, Color.BLACK))
//                    setBackgroundColor(R.color.colorOnSecondary)
                }
            }

            bindingMF.deleteBtn.setOnClickListener {
                dialog.dismiss()
                requestDeleteR(position = position)
            }

            return@setOnLongClickListener true
        }
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    private fun sendIntent(pos: Int, ref: String) {
        VideoPlayerActivity.position = pos
        val intent = Intent(context, VideoPlayerActivity::class.java)
        intent.putExtra("class", ref)
        ContextCompat.startActivity(context, intent, null)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(searchList: ArrayList<Video>) {
        videoList = ArrayList()
        videoList.addAll(searchList)
        notifyDataSetChanged()
    }

    //for requesting android 11 or higher storage permission
//    private fun requestPermissionR(){
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                if(!Environment.isExternalStorageManager()){
//                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//                    intent.addCategory("android.intent.category.DEFAULT")
//                    intent.data = Uri.parse("package:${context.applicationContext.packageName}")
//                    ContextCompat.startActivity(context, intent, null)
//                }
//            }
//    }

    private fun requestDeleteR(position: Int) {
        //list of videos to delete
        val uriList: List<Uri> =
            listOf(Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoList[position].id))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            //requesting for delete permission
            val pi = MediaStore.createDeleteRequest(context.contentResolver, uriList)
            (context as Activity).startIntentSenderForResult(pi.intentSender, 123,
                null, 0, 0, 0, null)
        } else {
            //for devices less than android 11
            val file = File(videoList[position].path)
            val builder = MaterialAlertDialogBuilder(context, R.style.alertDialog)
            builder.setTitle("Delete Video?")
                .setMessage(videoList[position].title)
                .setPositiveButton("Yes") { self, _ ->
                    if (file.exists() && file.delete()) {
                        MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                        updateDeleteUI(position = position)
                    }
                    self.dismiss()
                }
                .setNegativeButton("No") { self, _ -> self.dismiss() }
            val delDialog = builder.create()
            delDialog.show()
            delDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(
                MaterialColors.getColor(context, R.attr.themeColor, Color.BLACK)
            )
            delDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(
                MaterialColors.getColor(context, R.attr.themeColor, Color.BLACK)
            )
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateDeleteUI(position: Int) {
        when {
            VideoActivity.search -> {
//                MainActivity.dataChanged = true
                VideoActivity.videoList = getAllVideos(context)
                videoList.removeAt(position)
                notifyDataSetChanged()
            }
            isFolder -> {
//                MainActivity.dataChanged = true
                VideoActivity.videoList = getAllVideos(context)
                FoldersActivity.currentFolderVideos.removeAt(position)
                notifyDataSetChanged()
            }
            else -> {
                VideoActivity.videoList.removeAt(position)
                notifyDataSetChanged()
            }
        }
    }

    private fun requestWriteR() {
        //files to modify
        val uriList: List<Uri> =
            listOf(Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoList[newPosition].id))

        //requesting file write permission for specific files
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pi = MediaStore.createWriteRequest(context.contentResolver, uriList)
            (context as Activity).startIntentSenderForResult(pi.intentSender, 124,
                null, 0, 0, 0, null)
        } else renameFunction(newPosition)
    }

    private fun renameFunction(position: Int) {
        val customDialogRF = LayoutInflater.from(context).inflate(R.layout.rename_field,
            (context as Activity).findViewById(R.id.clVideoActivity), false)
        val bindingRF = RenameFieldBinding.bind(customDialogRF)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            dialogRF =
                MaterialAlertDialogBuilder(context, R.style.alertDialog).setView(customDialogRF)
                    .setCancelable(false)
                    .setPositiveButton("Rename") { self, _ ->
                        val currentFile = File(videoList[position].path)
                        val newName = bindingRF.renameField.text.toString()
                            .replace(".${currentFile.extension}", "")
                        val newFile = File(currentFile.parentFile,
                            newName.toString() + "." + currentFile.extension)
                        val parentDir = File(currentFile.parentFile!!.toURI())
                        if (currentFile.exists() && newName.toString().isNotEmpty()) {
                            try {
                                val fromUri =
                                    Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                        videoList[position].id)

                                ContentValues().also {
                                    it.put(MediaStore.Video.Media.IS_PENDING, 1)
                                    context.contentResolver.update(fromUri, it, null, null)
                                    it.clear()

                                    //updating file details
                                    it.put(MediaStore.Video.Media.DISPLAY_NAME, newName.toString())
                                    it.put(MediaStore.Video.Media.IS_PENDING, 0)
                                    context.contentResolver.update(fromUri, it, null, null)
                                    updateRenameUI(position,
                                        newName = "$newName.${currentFile.extension}",
                                        newFile = newFile)
                                }
                            } catch (e: IllegalArgumentException) {
                                e.printStackTrace()
                                try {
                                    renamePosition = position
                                    finalNewName = "$newName.${currentFile.extension}"
                                    val permittedStorage = appPref.getString(AppPref.PERMITTED_VIDEO_STORAGE).toString()
                                    val directories = permittedStorage.split("#@#")
                                    for (directory in directories){
                                        if (directories.isNotEmpty()){
                                            val uri = directory.split("@#@")
                                            if (uri[0] == parentDir.toString()){
                                                renameUsingSAF(Uri.parse(uri[1]))
                                                return@setPositiveButton
                                            }
                                        }
                                    }
                                    val builder =
                                        MaterialAlertDialogBuilder(context, R.style.alertDialog)
                                            .setTitle("Rename External Storage File")
                                            .setMessage("${videoList[position].title}\n\nTo Rename the Files from External SdCard Directory. Select the Directory in which the file is located")
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
//                                if(Environment.isExternalStorageManager()){
//                                    val fromUri = Uri.withAppendedPath(MediaStore.Video.Media.getContentUri(MediaStore.getExternalVolumeNames(context).toList()[1]),
//                                        videoList[position].id)
//                                    ContentValues().also {
//                                        it.put(MediaStore.Video.Media.IS_PENDING, 1)
//                                        context.contentResolver.update(fromUri, it, null, null)
//                                        it.clear()
//
//                                        //updating file details
//                                        it.put(MediaStore.Video.Media.DISPLAY_NAME, newName.toString())
//                                        it.put(MediaStore.Video.Media.IS_PENDING, 0)
//                                        context.contentResolver.update(fromUri, it, null, null)
//                                        updateRenameUI(position, newName = "$newName.${currentFile.extension}", newFile = newFile)
//                                    }
//                                }else{
//                                    val builder = MaterialAlertDialogBuilder(context,R.style.alertDialog)
//                                        .setTitle("Rename External Storage File")
//                                        .setMessage("${videoList[position].title}\n\nTo Rename the Files from External SdCard Directory. It requires All Files access from the App settings")
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
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        self.dismiss()
                    }
                    .setNegativeButton("Cancel") { self, _ ->
                        self.dismiss()
                    }
                    .create()
        } else {
            dialogRF =
                MaterialAlertDialogBuilder(context, R.style.alertDialog).setView(customDialogRF)
                    .setCancelable(false)
                    .setPositiveButton("Rename") { self, _ ->
                        val currentFile = File(videoList[position].path)
                        val newName = bindingRF.renameField.text.toString()
                            .replace(".${currentFile.extension}", "")
                        if (currentFile.exists() && newName.toString().isNotEmpty()) {
                            val newFile = File(currentFile.parentFile,
                                newName.toString() + "." + currentFile.extension)
                            if (currentFile.renameTo(newFile)) {
                                MediaScannerConnection.scanFile(context,
                                    arrayOf(newFile.toString()),
                                    arrayOf("video/*"),
                                    null)
                                updateRenameUI(position = position,
                                    newName = "$newName.${currentFile.extension}",
                                    newFile = newFile)
                            }
                        }
                        self.dismiss()
                    }
                    .setNegativeButton("Cancel") { self, _ ->
                        self.dismiss()
                    }
                    .create()
        }
        bindingRF.renameField.text = SpannableStringBuilder(videoList[newPosition].title)
        dialogRF.show()
        dialogRF.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(
            MaterialColors.getColor(context, R.attr.themeColor, Color.BLACK))
        dialogRF.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(
            MaterialColors.getColor(context, R.attr.themeColor, Color.BLACK))
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateRenameUI(position: Int, newName: String, newFile: File) {
        when {
            VideoActivity.search -> {
//                MainActivity.searchList[position].title = newName
//                MainActivity.searchList[position].path = newFile.path
//                MainActivity.searchList[position].artUri = Uri.fromFile(newFile)
//                notifyItemChanged(position)
                VideoActivity.videoList = getAllVideos(context)
                videoList = VideoActivity.videoList
                notifyDataSetChanged()
            }
            isFolder -> {
                FoldersActivity.currentFolderVideos[position].title = newName
                FoldersActivity.currentFolderVideos[position].path = newFile.path
                FoldersActivity.currentFolderVideos[position].artUri = Uri.fromFile(newFile)
                notifyItemChanged(position)
//                MainActivity.dataChanged = true
                VideoActivity.videoList = getAllVideos(context)
            }
            else -> {
                VideoActivity.videoList[position].title = newName
                VideoActivity.videoList[position].path = newFile.path
//                VideoActivity.videoList[position].artUri = Uri.fromFile(newFile)
                notifyItemChanged(position)
//                Log.d("CLEAR","else")
//                VideoActivity.videoList = getAllVideos(context)
//                videoList = VideoActivity.videoList
//                notifyDataSetChanged()
            }
        }
    }

    fun onResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            123 -> {
                if (resultCode == Activity.RESULT_OK) updateDeleteUI(newPosition)
            }
            124 -> if (resultCode == Activity.RESULT_OK) renameFunction(newPosition)
            23 -> {
                if (resultCode == Activity.RESULT_OK) {
                    val treeUri = data!!.data!!
                    context.contentResolver.takePersistableUriPermission(treeUri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    val currentFile = File(videoList[renamePosition].path)
                    val parentDir = File(currentFile.parentFile!!.toURI())
                    var permittedStorage = appPref.getString(AppPref.PERMITTED_VIDEO_STORAGE).toString()
                    permittedStorage += "$parentDir@#@$treeUri#@#"
                    Log.d("CLEAR","treeUri|| $treeUri")
                    appPref.setString(AppPref.PERMITTED_VIDEO_STORAGE,permittedStorage)
                    renameUsingSAF(treeUri)
                }
            }
        }
    }

    private fun renameUsingSAF(treeUri: Uri){
        val currentFile = File(videoList[renamePosition].path)
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
}