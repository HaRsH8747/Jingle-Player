package com.jingleplayer

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task
import com.jingleplayer.audiosection.AudioActivity
import com.jingleplayer.audiosection.AudioPlayerActivity
import com.jingleplayer.audiosection.exitApplication
import com.jingleplayer.databinding.ActivityHomeBinding
import com.jingleplayer.videosection.VideoActivity
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class HomeActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private lateinit var binding: ActivityHomeBinding
    private var hasReadPermissions: Boolean = false
    private var hasWritePermissions: Boolean = false
    private val REQUEST_CODE_READ_EXTERNAL_STORAGE = 10
    private val REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 11
    private val REQUEST_CODE_MANAGE_EXTERNAL_STORAGE = 12
    private lateinit var appUpdateManager: AppUpdateManager
    private val MY_REQUEST_CODE: Int = 99

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_JinglePlayer)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkUpdate()

        binding.btnShare.setOnClickListener { shareApplication() }
        binding.btnRate.setOnClickListener { rateApplication() }

        binding.btnAudio.setOnClickListener {
            if (hasWritePermissions){
                val intent = Intent(this, AudioActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
            }else{
                requestPermissions()
            }
        }

        binding.btnVideo.setOnClickListener {
            if (hasWritePermissions){
                val intent = Intent(this, VideoActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right,R.anim.slide_out_left)
            }else{
                requestPermissions()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requestPermissions()
    }

    var resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            val isDeleted: Boolean = deletePhotoFromInternalStorage()
        } else {
            deletePhotoFromInternalStorage()
        }
    }

    private fun saveToInternalStorage(bitmapImage: Bitmap) {
        val myPath = File(filesDir, "logo.png")
        Log.d("CLEAR", "save file: $myPath")
        var fos: FileOutputStream? = null
        try {
            fos = FileOutputStream(myPath)
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            try {
                fos!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun loadImageFromStorage(): Uri? {
        return try {
            val myPath = File(filesDir, "logo.png")
            Log.d("CLEAR", "load file: $myPath")
            FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".provider",
                myPath)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun deletePhotoFromInternalStorage(): Boolean {
        return try {
            val file = File(filesDir, "logo.png")
            file.delete()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun shareApplication() {
        try {
            val bmp =
                BitmapFactory.decodeResource(this.resources, R.drawable.share_logo)
            saveToInternalStorage(bmp)
            val uri: Uri? = loadImageFromStorage()
            grantUriPermission(BuildConfig.APPLICATION_ID,
                uri,
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "image/png"
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            shareIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Jingle Player")
            val shareMessage =
                "\nPlay all your music and video files using this amazing Application.\n\nhttps://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}".trimIndent()
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
            resultLauncher.launch(Intent.createChooser(shareIntent, "Choose one"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun rateApplication() {
        val reviewManager = ReviewManagerFactory.create(this)
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task: Task<ReviewInfo?> ->
            if (task.isSuccessful) {
                Log.d("CLEAR", "request Successful")
                val reviewInfo = task.result
                val flow =
                    reviewManager.launchReviewFlow(this, reviewInfo)
                flow.addOnCompleteListener {
                    Log.d("CLEAR",
                        "launch Successful")
                }
            } else {
                goToPlayStore()
            }
        }
        request.addOnFailureListener {
            goToPlayStore()
        }
    }

    private fun goToPlayStore() {
        val uri = Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID)
        val goToMarket = Intent(Intent.ACTION_VIEW, uri)
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        try {
            startActivity(goToMarket)
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("http://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)))
        }
    }

    private val listener: InstallStateUpdatedListener = InstallStateUpdatedListener { installState ->
        if (installState.installStatus() == InstallStatus.DOWNLOADED) {
            // After the update is downloaded, show a notification
            // and request user confirmation to restart the app.
            Log.d("CLEAR", "An update has been downloaded")
            appUpdateManager.completeUpdate()
        }
    }

    private fun checkUpdate() {
        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        // Checks that the platform will allow the specified type of update.
        Log.d("CLEAR", "Checking for updates")
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                appUpdateManager.registerListener(listener)
                appUpdateManager.startUpdateFlowForResult(
                    // Pass the intent that is returned by 'getAppUpdateInfo()'.
                    appUpdateInfo,
                    // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                    AppUpdateType.FLEXIBLE,
                    // The current activity making the update request.
                    this,
                    // Include a request code to later monitor this update request.
                    MY_REQUEST_CODE)
                Log.d("CLEAR", "Update available")
            } else {
                Log.d("CLEAR", "No Update available")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!AudioPlayerActivity.isPlaying && AudioPlayerActivity.musicService != null){
            exitApplication()
        }
    }

    private fun requestPermissions() {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            hasWritePermissions = true
        }else{
            EasyPermissions.requestPermissions(
                this,
                "You need to accept Write permissions to use this app",
                REQUEST_CODE_WRITE_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        hasWritePermissions = hasWritePermissions || minSdk29
//        if (EasyPermissions.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
//            hasReadPermissions = true
//        }else{
//            EasyPermissions.requestPermissions(
//                this,
//                "You need to accept Read permissions to use this app",
//                REQUEST_CODE_READ_EXTERNAL_STORAGE,
//                Manifest.permission.READ_EXTERNAL_STORAGE
//            )
//        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        if (requestCode == REQUEST_CODE_WRITE_EXTERNAL_STORAGE){
            lifecycleScope.launch {
                val minSdk30 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                hasWritePermissions = true || minSdk30
            }
        }
        if (requestCode == REQUEST_CODE_READ_EXTERNAL_STORAGE){
            lifecycleScope.launch {
                hasReadPermissions = true
            }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermissions()
        }
    }
}