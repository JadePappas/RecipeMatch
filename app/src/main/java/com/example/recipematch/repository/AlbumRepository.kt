package com.example.recipematch.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.recipematch.model.Album
import com.google.firebase.firestore.FirebaseFirestore

class AlbumRepository {
    private val db = FirebaseFirestore.getInstance()
    private val albumsCollection = db.collection("albums")

    fun getAlbums(userId: String): LiveData<List<Album>> {
        val albums = MutableLiveData<List<Album>>()
        albumsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    albums.value = snapshot.toObjects(Album::class.java)
                }
            }
        return albums
    }

    fun addAlbum(album: Album, onComplete: (Boolean) -> Unit) {
        albumsCollection.add(album)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun updateAlbum(album: Album, onComplete: (Boolean) -> Unit) {
        if (album.albumId.isNotEmpty()) {
            albumsCollection.document(album.albumId).set(album)
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }

    fun deleteAlbum(albumId: String, onComplete: (Boolean) -> Unit) {
        if (albumId.isNotEmpty()) {
            albumsCollection.document(albumId).delete()
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }
}