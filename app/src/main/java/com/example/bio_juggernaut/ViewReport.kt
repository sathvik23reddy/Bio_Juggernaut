package com.example.bio_juggernaut

import android.os.Bundle
import android.widget.ImageView
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.bio_juggernaut.doas.PostDao
import com.example.bio_juggernaut.models.Post
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.concurrent.schedule

class ViewReport : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var gMap: GoogleMap
    private lateinit var postDao: PostDao
    private lateinit var latitude: String
    private lateinit var longitude: String
    private lateinit var imgView: PhotoView
    private lateinit var imgURI: String
    private lateinit var post: Post
    private val db = FirebaseFirestore.getInstance()
    private val postCollections = db.collection("posts")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_report)
        postDao = PostDao()

        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener{
            finish()
        }

        //Get PostID from Main Activity
        val postID = intent.getStringExtra("postID")!!

        //Setup Map Fragment
        val supportMapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.viewLocation) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        //Resolved Switch
        val isResolved = findViewById<Switch>(R.id.isResolved)

        //Get Post Object from DocumentSnapshot and use required variables
        GlobalScope.launch {
            post = postDao.getPostById(postID).await().toObject(Post::class.java)!!
            latitude = post.latitude
            longitude = post.longitude
            imgURI = post.imgUri
            runOnUiThread{
                isResolved.isChecked = post.isResolved
            }

        }

        //Switch Listener
        isResolved.setOnCheckedChangeListener { _, isChecked ->
            GlobalScope.launch {
                post.isResolved = isChecked
                postCollections.document(postID).set(post)
            }
        }


    }

    //Drop Pin on Map + Load Image into PhotoView(Pinch Zoomable - Chrisbanes's Library)
    override fun onMapReady(p0: GoogleMap) {
        gMap = p0
        Timer().schedule(2000) {
            runOnUiThread{
                //Pin on Map
                val markerOptions = MarkerOptions()
                val pos = LatLng(latitude.toDouble(), longitude.toDouble())
                markerOptions.position(pos)
                val s:String = pos.latitude.toString() + " : " + pos.longitude.toString()
                markerOptions.title(s)
                gMap.clear()
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 17F))
                gMap.addMarker(markerOptions)

                //Image into PhotoView
                imgView = findViewById(R.id.imgView)
                Glide.with(applicationContext).load(imgURI).into(imgView)

            }
        }
    }
}