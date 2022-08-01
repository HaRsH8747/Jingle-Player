package com.jingleplayer.videosection

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.jingleplayer.R
import com.jingleplayer.databinding.FragmentVideosBinding

class VideosFragment : Fragment() {

    lateinit var adapter: VideoAdapter
    private lateinit var binding: FragmentVideosBinding
    private var spinnerIndex = 0
    private val searchType =
        arrayOf("Recent", "Oldest", "Name(A to Z)", "Name(Z to A)", "File Size(Smallest)", "File Size(Largest)")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentVideosBinding.inflate(layoutInflater,container,false)

        binding.rvVideo.setHasFixedSize(true)
        binding.rvVideo.setItemViewCacheSize(10)
        binding.rvVideo.layoutManager = LinearLayoutManager(requireContext())
        adapter = VideoAdapter(requireContext(), VideoActivity.videoList)
        binding.rvVideo.adapter = adapter

        val spinnerAdapter: ArrayAdapter<String> =
            ArrayAdapter<String>(requireContext(), R.layout.spinner_item, searchType)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinner.adapter = spinnerAdapter
        binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(adapterView: AdapterView<*>?, view: View, i: Int, l: Long) {
                spinnerIndex = i
                if(VideoActivity.sortValue != spinnerIndex){
                    VideoActivity.sortValue = spinnerIndex
                    VideoActivity.videoList = getAllVideos(requireContext())
                    adapter.updateList(VideoActivity.videoList)
                }
            }
            override fun onNothingSelected(adapterView: AdapterView<*>?) {}
        }

        binding.search.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun afterTextChanged(p0: Editable?) {
                binding.search.removeTextChangedListener(this)
                if(binding.search.text?.isNotEmpty() == true){
                    VideoActivity.searchList = ArrayList()
                    for(video in VideoActivity.videoList){
                        if(video.title.lowercase().contains(binding.search.text.toString().lowercase()))
                            VideoActivity.searchList.add(video)
                    }
                    VideoActivity.search = true
                    adapter.updateList(searchList = VideoActivity.searchList)
                }else{
                    adapter.updateList(VideoActivity.videoList)
                }
                binding.search.addTextChangedListener(this)
            }
        })

        binding.root.setOnRefreshListener {
            VideoActivity.videoList = getAllVideos(requireContext())
            if (binding.search.text?.isNotEmpty() == true){
                adapter.updateList(searchList = VideoActivity.searchList)
            }else{
                adapter.updateList(getAllVideos(requireContext()))
            }
            binding.root.isRefreshing = false
        }

        binding.nowPlayingBtn.setOnClickListener{
            val intent = Intent(requireContext(), VideoPlayerActivity::class.java)
            intent.putExtra("class", "NowPlaying")
            startActivity(intent)
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        if(VideoPlayerActivity.position != -1) binding.nowPlayingBtn.visibility = View.VISIBLE
    }

}