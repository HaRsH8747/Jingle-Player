package com.jingleplayer.videosection

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.jingleplayer.databinding.FragmentFoldersBinding

class FoldersFragment : Fragment() {

    private lateinit var binding: FragmentFoldersBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentFoldersBinding.inflate(layoutInflater,container,false)
        binding.FoldersRV.setHasFixedSize(true)
        binding.FoldersRV.setItemViewCacheSize(10)
        binding.FoldersRV.layoutManager = LinearLayoutManager(requireContext())
        binding.FoldersRV.adapter = FoldersAdapter(requireContext(), VideoActivity.folderList)
        return binding.root
    }
}