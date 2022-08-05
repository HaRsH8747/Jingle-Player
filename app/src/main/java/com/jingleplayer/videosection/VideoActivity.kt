package com.jingleplayer.videosection

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.fragment.app.Fragment
import com.jingleplayer.R
import com.jingleplayer.databinding.ActivityVideoBinding

class VideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var currentFragment: Fragment

    companion object {
        lateinit var videoList: ArrayList<Video>
        lateinit var folderList: ArrayList<Folder>
        lateinit var searchList: ArrayList<Video>
        lateinit var videoActivity: VideoActivity
        var search: Boolean = false
        var sortValue: Int = 0

        val sortList = arrayOf(
            MediaStore.Video.Media.DATE_MODIFIED + " DESC",
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DISPLAY_NAME + " DESC",
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.SIZE + " DESC"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        videoActivity = this
        folderList = ArrayList()
        videoList = getAllVideos(this)
        setFragment(VideosFragment(),1)
        binding.back.setOnClickListener {
            onBackPressed()
        }

//        if (requestRuntimePermission()) {
//
////            runnable = Runnable {
////                if (dataChanged) {
////                    videoLis t = getAllVideos(this)
////                    dataChanged = false
////                    adapterChanged = true
////                }
////                Handler(Looper.getMainLooper()).postDelayed(runnable!!, 200)
////            }
////            Handler(Looper.getMainLooper()).postDelayed(runnable!!, 0)
//
//        } else {
//            folderList = ArrayList()
//            videoList = ArrayList()
//        }

        binding.bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.videoView -> setFragment(VideosFragment(),1)
                R.id.foldersView -> setFragment(FoldersFragment(),2)
            }
            return@setOnItemSelectedListener true
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_right)
    }

    private fun setFragment(fragment: Fragment, i: Int){
//        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentFL) as NavHostFragment
//        val navController = navHostFragment.navController
        currentFragment = fragment
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentFL, fragment)
        transaction.disallowAddToBackStack()
        if (i == 2){
//            navController.navigate(R.id.action_videosFragment_to_foldersFragment)
            transaction.setCustomAnimations(R.anim.slide_in_right,R.anim.slide_out_left)
        }else{
//            navController.popBackStack()
            transaction.setCustomAnimations(R.anim.slide_in_left,R.anim.slide_out_right)
        }
        transaction.commit()
    }

//    private fun requestRuntimePermission(): Boolean{
//
//        //requesting storage permission for only devices less than api 28
//        if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.P){
//            if(ActivityCompat.checkSelfPermission(this,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
//                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),13)
//                return false
//            }
//        }else{
//            //read external storage permission for devices higher than android 10 i.e. api 29
//            if(ActivityCompat.checkSelfPermission(this,
//                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
//                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),14)
//                return false
//            }
//        }
//        return true
//    }

//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if(requestCode == 13) {
//            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
//                folderList = ArrayList()
//                videoList = getAllVideos(this)
//                setFragment(VideosFragment(), 1)
//            }
//            else Snackbar.make(binding.root, "Storage Permission Needed!!", 5000)
//                .setAction("OK"){
//                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),13)
//                }
//                .show()
////                ActivityCompat.requestPermissions(this, arrayOf(WRITE_EXTERNAL_STORAGE),13)
//        }
//
//        //for read external storage permission
//        if(requestCode == 14) {
//            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
//                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
//                folderList = ArrayList()
//                videoList = getAllVideos(this)
//                setFragment(VideosFragment(), 1)
//            }
//            else Snackbar.make(binding.root, "Storage Permission Needed!!", 5000)
//                .setAction("OK"){
//                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),14)
//                }
//                .show()
////            else
////                ActivityCompat.requestPermissions(this, arrayOf(READ_EXTERNAL_STORAGE),14)
//        }
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        (currentFragment as VideosFragment).adapter.onResult(requestCode, resultCode,data)
    }
}