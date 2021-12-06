package com.example.bio_juggernaut.doas

import com.example.bio_juggernaut.models.User
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class UserDao {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    //Used to add user to FS-DB
    fun addUser(user: User?) {
        user?.let {
            GlobalScope.launch(Dispatchers.IO) {
                usersCollection.document(user.uid).set(it)
            }
        }
    }

    //Retrieves DocumentSnapshot of Required User which is later converted to User Object
    fun getUserById(uId: String): Task<DocumentSnapshot> {
        return usersCollection.document(uId).get()
    }
}