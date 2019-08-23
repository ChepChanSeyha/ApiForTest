package com.example.apifortesting

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.apifortesting.Model.ModelResponse
import com.example.apifortesting.RetrofitClient.RetrofitMaps
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit.*
import retrofit2.converter.gson.GsonConverterFactory

@Suppress("DEPRECATED_IDENTITY_EQUALS")
open class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private lateinit var origin: LatLng
    private lateinit var dest: LatLng
    private lateinit var markerPoints: ArrayList<LatLng>
    internal lateinit var showDistanceDuration: TextView
    internal var line: Polyline? = null

    // Checking if Google Play Services Available or not
    private val isGooglePlayServicesAvailable: Boolean
        get() {
            val googleAPI = GoogleApiAvailability.getInstance()
            val result = googleAPI.isGooglePlayServicesAvailable(this)
            if (result != ConnectionResult.SUCCESS) {
                if (googleAPI.isUserResolvableError(result)) {
                    googleAPI.getErrorDialog(this, result,
                        0).show()
                }
                return false
            }
            return true
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        showDistanceDuration = findViewById(R.id.show_distance_time)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission()
        }

        // Initializing
        markerPoints = ArrayList()

        //show error dialog if Google Play Services not available
        if (!isGooglePlayServicesAvailable) {
            Log.d("onCreate", "Google Play Services not available. Ending Test case.")
            finish()
        } else {
            Log.d("onCreate", "Google Play Services available. Continuing.")
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val modelTown = LatLng(28.7158727, 77.1910738)
        mMap!!.addMarker(MarkerOptions().position(modelTown).title("Marker in Sydney"))
        mMap!!.moveCamera(CameraUpdateFactory.newLatLng(modelTown))
        mMap!!.animateCamera(CameraUpdateFactory.zoomTo(11F))

        // Setting onclick event listener for the map
        mMap!!.setOnMapClickListener { point ->
            // clearing map and generating new marker points if user clicks on map more than two times
            if (markerPoints.size > 1) {
                mMap!!.clear()
                markerPoints.clear()
                markerPoints = ArrayList()
                showDistanceDuration.text = ""
            }

            // Adding new item to the ArrayList
            markerPoints.add(point)

            // Creating MarkerOptions
            val options = MarkerOptions()

            // Setting the position of the marker
            options.position(point)

            if (markerPoints.size == 1) {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            } else if (markerPoints.size == 2) {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            }


            // Add new marker to the Google Map Android API V2
            mMap!!.addMarker(options)

            // Checks, whether start and end locations are captured
            if (markerPoints.size >= 2) {
                origin = markerPoints[0]
                dest = markerPoints[1]
            }
        }

        val btnDriving = findViewById<Button>(R.id.btnDriving)
        btnDriving.setOnClickListener { buildRetrofitAndGetResponse() }

        val btnWalk = findViewById<Button>(R.id.btnWalk)
        btnWalk.setOnClickListener { buildRetrofitAndGetResponse() }
    }

    private fun buildRetrofitAndGetResponse() {

        val url = "http://10.10.40.29:8093/api/"

        val retrofit = Builder()
            .baseUrl(url)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create<RetrofitMaps>(RetrofitMaps::class.java)

        val call = service.getDistanceDuration()

        call.enqueue(object : Callback<ModelResponse> {
            @SuppressLint("SetTextI18n")
            override fun onResponse(call: Call<ModelResponse>, response: Response<ModelResponse>) {

                try {
                    //Remove previous line from map
                    if (line != null) {
                        line!!.remove()
                    }
                    // This loop will go through all the results and add marker on each location.
                    for (i in response.body()?.route!!.indices) {
                        val distance = response.body()!!.route?.get(0)
                        showDistanceDuration.text = "Distance:$distance"
                        val encodedString = response.body()!!.route.toString()
                        val list = decodePoly(encodedString)
                        line = mMap!!.addPolyline(
                            PolylineOptions()
                            .addAll(list)
                            .width(20F)
                            .color(Color.RED)
                            .geodesic(true)
                        )
                    }
                } catch (e: Exception) {
                    Log.d("onResponse", "There is an error")
                    e.printStackTrace()
                }

            }

            override fun onFailure(call: Call<ModelResponse>, t: Throwable) {
                Log.d("onFailure", t.toString())
            }
        })

    }

    private fun decodePoly(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng(lat.toDouble() / 1E5,
                lng.toDouble() / 1E5)
            poly.add(p)
        }

        return poly
    }

    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !== PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION)


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION)
            }
            return false
        } else {
            return true
        }
    }

    companion object {

        const val MY_PERMISSIONS_REQUEST_LOCATION = 99
    }
}
