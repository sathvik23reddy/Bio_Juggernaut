package com.example.bio_juggernaut

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bio_juggernaut.doas.PostDao
import com.example.bio_juggernaut.models.Post
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity(), IPostAdapter {
    private lateinit var signOutButton: Button
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private lateinit var reportEventButton: FloatingActionButton
    private lateinit var postDao: PostDao
    private lateinit var adapter: PostAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        signOutButton = findViewById(R.id.signOutButton)
        signOutButton.setOnClickListener{
            auth = Firebase.auth
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)
            signUserOut()
        }

        reportEventButton = findViewById(R.id.reportEvent)
        reportEventButton.setOnClickListener{
            val ReportIntent = Intent(this, ReportEvent::class.java)
            startActivity(ReportIntent)
        }

        setUpRecyclerView()
    }

    private fun signUserOut() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            val SignInIntent= Intent(this, SignInActivity::class.java)
            startActivity(SignInIntent)
            finish()
        }
    }

    private fun setUpRecyclerView() {
        recyclerView  = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        postDao = PostDao()
        val postsCollections = postDao.postCollections
        //Query to sort by "Not Resolved" and then by "Time Created"
        val query = postsCollections.orderBy("resolved", Query.Direction.ASCENDING).orderBy("createdAt",Query.Direction.DESCENDING)
        val recyclerViewOptions = FirestoreRecyclerOptions.Builder<Post>().setQuery(query, Post::class.java).build()
        adapter = PostAdapter(recyclerViewOptions, this)
        recyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onPostClicked(postId: String) {
        val i= Intent(this, ViewReport::class.java)
        i.putExtra("postID", postId)
        setResult(RESULT_OK, i);
        startActivity(i)
    }

}