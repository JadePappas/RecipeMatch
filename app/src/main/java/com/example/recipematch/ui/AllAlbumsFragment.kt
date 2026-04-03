package com.example.recipematch.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipematch.R
import com.example.recipematch.viewmodel.AlbumViewModel

class AllAlbumsFragment : Fragment() {

    private lateinit var albumViewModel: AlbumViewModel
    private lateinit var albumAdapter: AlbumAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.all_albums_fragment, container, false)
        albumViewModel = ViewModelProvider(requireActivity()).get(AlbumViewModel::class.java)

        val btnBack = view.findViewById<ImageButton>(R.id.btn_albums_back)
        val rvAlbums = view.findViewById<RecyclerView>(R.id.rv_all_albums)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        rvAlbums.layoutManager = GridLayoutManager(context, 2)
        albumAdapter = AlbumAdapter { album ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AlbumDetailFragment.newInstance(album))
                .addToBackStack(null)
                .commit()
        }
        rvAlbums.adapter = albumAdapter

        albumViewModel.albums.observe(viewLifecycleOwner) { albums ->
            albumAdapter.submitList(albums)
        }

        return view
    }
}