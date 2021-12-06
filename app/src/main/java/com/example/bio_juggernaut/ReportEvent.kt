package com.example.bio_juggernaut

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.bio_juggernaut.doas.PostDao
import com.example.bio_juggernaut.doas.UserDao
import com.example.bio_juggernaut.models.Post
import com.example.bio_juggernaut.models.User
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.concurrent.schedule


class ReportEvent : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var gMap: GoogleMap
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_LOCATION_LATLNG = 2
    private lateinit var imgView :ImageView
    private lateinit var imgUri: Uri
    private lateinit var imgBitMap: Bitmap
    private var latitude:String = "12.967431"
    private var longitude: String = "77.713795"
    private lateinit var postDao: PostDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_event)

        //BackButton within ActionBar
        val backButton = findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener{
            finish()
        }

        //Map Fragment Instantiation
        val supportMapFragment: SupportMapFragment = supportFragmentManager.findFragmentById(R.id.viewLocation) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        //Preview ImgView
        imgView = findViewById(R.id.previewImg)

        //Camera
        val openCamera: ImageView = findViewById(R.id.openCamera)
        openCamera.setOnClickListener{
            dispatchTakePictureIntent()
        }

        //Choose From Gallery
        val chooseFromGallery: ImageView= findViewById(R.id.chooseFromGallery)
        chooseFromGallery.setOnClickListener{
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.setType("image/*")
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
    }

    //Camera Intent
    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } catch (e: ActivityNotFoundException) {
            Handler(Looper.getMainLooper()).post(Runnable {
                Toast.makeText(this, "Camera Not Found", Toast.LENGTH_SHORT).show()
            })
        }
    }

    //Uploads Img to FB Storage, segregated by userID, date, time
    @RequiresApi(Build.VERSION_CODES.O)
    private fun uploadImg(bitmap: Bitmap) {
        val currentDateTime = LocalDateTime.now()
        val userID = Firebase.auth.currentUser?.uid
        val baos = ByteArrayOutputStream()
        val date:String = currentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val time:String = currentDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val ref = FirebaseStorage.getInstance().reference.child("images/$userID/$date-$time")
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val img = baos.toByteArray()
        val upload = ref.putBytes(img)

        upload.addOnCompleteListener{ uploadTask ->
            if(uploadTask.isSuccessful){
                ref.downloadUrl.addOnCompleteListener{urlTask ->
                    urlTask.result?.let {
                        imgUri = it
                    }
                }
            }
        }

    }

    //onActivityResult for Camera Intent(Code:1) and Map Intent(Code:2)
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //Return from Camera Intent
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try{
                imgBitMap= data?.extras?.get("data") as Bitmap
            }
            catch(e: Exception){
                val inputStream: InputStream? = contentResolver.openInputStream(data?.data!!)
                imgBitMap=BitmapFactory.decodeStream(inputStream)
            }

            imgView.setImageBitmap(imgBitMap)
            uploadImg(imgBitMap)
        }

        //Return from Map Intent
        else if(requestCode == REQUEST_LOCATION_LATLNG && resultCode == RESULT_OK){
            latitude = data!!.getStringExtra("Latitude").toString()
            longitude = data!!.getStringExtra("Longitude").toString()
            //Places new coordinates on Map Fragment
            Timer().schedule(1000) {
                runOnUiThread{
                    val markerOptions = MarkerOptions()
                    val pos = LatLng(latitude.toDouble(), longitude.toDouble())
                    markerOptions.position(pos)
                    val s:String = pos.latitude.toString() + " : " + pos.longitude.toString()
                    markerOptions.title(s)
                    gMap.clear()
                    gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 17F))
                    gMap.addMarker(markerOptions)


                }
            }

        }
    }

    //Launch Map Fragment
    fun openMapsFragment(view: View) {
        val mapsFragment = Intent(this, MapActivity::class.java)
        startActivityForResult(mapsFragment, REQUEST_LOCATION_LATLNG)
    }

    //Gets Address using Geocoder
    private fun getAddress(latLng: LatLng): String {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses: List<Address>?
        val address: Address?
        val addressText: String

        addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

        if (addresses.isNotEmpty()) {
            address = addresses[0]
            addressText = address.getAddressLine(0)
        } else{
            addressText = "Click to View Location"
        }
        return addressText
    }

    //Posts Report to FS-DB using Co-routines
    fun postReport(view: View) {
        val reportTitle: com.google.android.material.textfield.TextInputEditText = findViewById(R.id.reportTitle)
        val text : String = reportTitle.text.toString()
        val auth = Firebase.auth
        val currentUserId = auth.currentUser!!.uid
        val userDao = UserDao()
        val currentTime = System.currentTimeMillis()
        val address = getAddress(LatLng(latitude.toDouble(), longitude.toDouble()))
        GlobalScope.launch {
            val user = userDao.getUserById(currentUserId).await().toObject(User::class.java)!!
            val post  = Post(text, user, currentTime, latitude, longitude, imgUri.toString(), address, false)
            postDao = PostDao()
            postDao.addPost(post)
        }
        finish()
    }


    override fun onMapReady(p0: GoogleMap) {
        gMap = p0
        Timer().schedule(1000) {
            runOnUiThread{
                val markerOptions = MarkerOptions()
                val pos = LatLng(latitude.toDouble(), longitude.toDouble())
                markerOptions.position(pos)
                val s:String = pos.latitude.toString() + " : " + pos.longitude.toString()
                markerOptions.title(s)
                gMap.clear()
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 17F))
                gMap.addMarker(markerOptions)


            }
        }
    }

}