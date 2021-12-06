package com.example.bio_juggernaut.doas

import com.example.bio_juggernaut.models.Post
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

class PostDao {

    val db = FirebaseFirestore.getInstance()
    val postCollections = db.collection("posts")

    //Posts report to FS-DB
    fun addPost(post:Post) {
        postCollections.document().set(post)
    }

    //Retrieves DocumentSnapshot of Required Post which is later converted to Post Object
    fun getPostById(postId: String): Task<DocumentSnapshot> {
        return postCollections.document(postId).get()
    }


}