package com.els.imaps

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.els.imaps.data.ThemeData
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.places.ui.PlacePicker
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.IOException

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener, PopupMenu.OnMenuItemClickListener {

    override fun onMarkerClick(p0: Marker?) = false

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    lateinit var title: String

    private lateinit var saveData: ThemeData
    private var backPressedTime: Long = 0
    private var backToast: Toast? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
        private const val PLACE_PICKER_REQUEST = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        saveData = ThemeData(this)
        if (saveData.loadDarkModeState() == true) {
            setTheme(R.style.DarkTheme)
        }else
            setTheme(R.style.AppTheme)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //Location callback setup
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                lastLocation = p0!!.lastLocation
                placeMaker(LatLng(lastLocation.latitude, lastLocation.longitude))
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createLocationRequest()

        //Bottom Navigation
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottom_navigation_bar)

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.my_location -> {
                    val myLocation = title
                    Toast.makeText(this, myLocation, Toast.LENGTH_SHORT).show()
                }

                R.id.share_address -> {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.type = "type/plain"
                    val shareAddress = "Here's my current address"

                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareAddress)

                    startActivity(Intent.createChooser(shareIntent, "Share my location"))


                }
            }
            true
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        checkPermission()

        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                lastLocation = location

                val currentLocation = LatLng(location.latitude, location.longitude)
                placeMaker(currentLocation)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 12.0f))
                mMap.uiSettings.isZoomControlsEnabled = true
            }
        }
        //Setting map type
        mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
    }

    //Check if users have given permission to ACCESS_FINE LOCATION
    private fun checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
    }

    //Adding icon to current location
    private fun placeMaker(latLng: LatLng) {
        val markerOptions = MarkerOptions().position(latLng)
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(resources, R.mipmap.mymarker)))

        //Tittle
         title = getAddress(latLng)
        markerOptions.title(title)
        mMap.addMarker(markerOptions)
    }

    //Getting user's current address
    private fun getAddress(latLng: LatLng): String {
        val geoCoder =  Geocoder(this)
        val addresses: List<Address>?
        val address: Address
        var addressText = ""

        try {
            addresses = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                address = addresses[0]

                for (i in 0 .. address.maxAddressLineIndex) {
                    addressText += if (i == 0) address.getAddressLine(i) else "\n"+ address.getAddressLine(i)
                }
            }
        }catch (e: IOException) {
            Toast.makeText(this, "Poor internet connections", Toast.LENGTH_SHORT).show()
        }
        return addressText
    }

    //Receiving location updates
    private fun startLocationUpdates() {
        checkPermission()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()

        locationRequest.interval = 60000

        locationRequest.fastestInterval = 300000

        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)

        //Check state of user's location settings
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                try {
                    e.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                }catch (e: IntentSender.SendIntentException) {
                    Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadPlacePicker() {
        val builder = PlacePicker.IntentBuilder()

        try {
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST)
        }catch (e: GooglePlayServicesRepairableException) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
        }catch (e: GooglePlayServicesNotAvailableException) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    //Changing map layer (popup menu)
    fun showDialog(view: View?) {
        val popup = PopupMenu(this, view)
        popup.setOnMenuItemClickListener(this)
        popup.inflate(R.menu.map_type)
        popup.show()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.normal_layer -> {
                Toast.makeText(this, "Normal", Toast.LENGTH_SHORT).show()
                mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                true
            }
            R.id.hybrid_layer -> {
                Toast.makeText(this, "Hybrid", Toast.LENGTH_SHORT).show()
                mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
                true
            }
            R.id.satellite_layer -> {
                Toast.makeText(this, "Satellite", Toast.LENGTH_SHORT).show()
                mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                true
            }
            R.id.terrain_layer -> {
                Toast.makeText(this, "Terrain", Toast.LENGTH_SHORT).show()
                mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                true
            }
            R.id.none_layer -> {
                Toast.makeText(this, "None", Toast.LENGTH_SHORT).show()
                mMap.mapType = GoogleMap.MAP_TYPE_NONE
                true
            }

            else -> false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == PLACE_PICKER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                val place = PlacePicker.getPlace(this, data)
                var addressText = place.name.toString()
                addressText += '\n' + place.name.toString()

                placeMaker(place.latLng)
            }
        }

        if(resultCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.search_location -> {
                loadPlacePicker()
            }

            R.id.app_settings -> {
                startActivity(Intent(this, Settings::class.java))
                overridePendingTransition(R.anim.zoom_in, R.anim.zoom_out)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }

    override fun onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            backToast?.cancel()
            super.onBackPressed()
            return
        }else {
            backToast = Toast.makeText(baseContext, "Press again to exit App", Toast.LENGTH_SHORT)
            backToast?.show()
        }

        backPressedTime = System.currentTimeMillis()
    }
}