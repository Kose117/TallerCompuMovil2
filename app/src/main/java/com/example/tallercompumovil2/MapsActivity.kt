package com.example.tallercompumovil2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.SearchView
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.tallercompumovil2.databinding.ActivityMapsBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {

    private lateinit var map: GoogleMap
    private val locationPermissionName = Manifest.permission.ACCESS_FINE_LOCATION
    private lateinit var location: FusedLocationProviderClient
    private lateinit var binding: ActivityMapsBinding
    private var currentLocationMarker: Marker? = null
    private var lastLocation: Location? = null
    private val locations = mutableListOf<LatLng>()
    private var primerZoom = false

    private val permissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback { isGranted ->
            if (isGranted) {
                startLocationUpdates()
            }
        })

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private val umbralBajo = 50f

    private lateinit var mGeocoder: Geocoder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        location = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = createLocationRequest()
        locationCallback = createLocationCallback()

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        permissionRequest.launch(locationPermissionName)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }

        mGeocoder = Geocoder(this)

        binding.buscador.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { findAddress(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun findAddress(addressString: String) {
        if (addressString.isNotEmpty()) {
            try {
                val addresses = mGeocoder.getFromLocationName(addressString, 2)
                if (addresses != null && addresses.isNotEmpty()) {
                    val addressResult = addresses[0]
                    val position = LatLng(addressResult.latitude, addressResult.longitude)
                    map.addMarker(
                        MarkerOptions().position(position)
                            .title(addressResult.featureName)
                            .snippet(addressResult.getAddressLine(0))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    )
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))

                    lastLocation?.let { currentLocation ->
                        val markerLocation = Location("").apply {
                            latitude = addressResult.latitude
                            longitude = addressResult.longitude
                        }
                        val distance = currentLocation.distanceTo(markerLocation) / 1000 // Convertir a kilómetros
                        val distanceString = String.format(Locale.getDefault(), "%.2f km", distance)
                        Toast.makeText(this, "Distancia al marcador: $distanceString km", Toast.LENGTH_SHORT).show()
                    } ?: Toast.makeText(this, "Ubicación actual no disponible", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Error procesando la dirección: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "La dirección está vacía", Toast.LENGTH_SHORT).show()
        }
    }

    fun writeJSONObject(location: Location) {
        val myLocation = MyLocation(Date(System.currentTimeMillis()), location.latitude, location.longitude)
        val jsonObject = myLocation.toJSON()
        val filename = "locations.json"
        val file = File(baseContext.getExternalFilesDir(null), filename)

        // Utilizar JSONArray para acumular ubicaciones y mantener el formato del archivo
        val locationsJsonArray: JSONArray
        if (file.exists()) {
            val content = file.readText()
            locationsJsonArray = JSONArray(content)
        } else {
            locationsJsonArray = JSONArray()
        }

        locationsJsonArray.put(jsonObject)

        file.writeText(locationsJsonArray.toString()) // Reescribe el archivo con el nuevo array
        Log.i("LOCATION", "File modified at path: " + file.absolutePath)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (::map.isInitialized && event.sensor.type == Sensor.TYPE_LIGHT) {
            val lightValue = event.values[0]
            if (lightValue < umbralBajo) {
                map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_night))
            } else {
                map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_day))
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.setOnMapLongClickListener { latLng ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Nuevo marcador")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
            locations.add(latLng)
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

            lastLocation?.let { lastKnownLocation ->
                val markerLocation = Location("").apply {
                    latitude = latLng.latitude
                    longitude = latLng.longitude
                }
                val distance = lastKnownLocation.distanceTo(markerLocation) / 1000 // Convertir a kilómetros
                val distanceString = String.format(Locale.getDefault(), "%.2f km", distance)
                Toast.makeText(this, "Distancia al marcador: $distanceString", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(this, "Ubicación actual no disponible", Toast.LENGTH_SHORT).show()
            }

            if (lastLocation == null) {
                startLocationUpdates()
            }
        }
    }

    private fun createLocationCallback(): LocationCallback {
        return object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                result.locations.forEach { location ->
                    // Llamar a updateLocationUI sin importar la distancia recorrida
                    updateLocationUI(location)

                    // Escribir en JSON si el usuario se ha movido más de 30 metros
                    if (lastLocation == null || lastLocation!!.distanceTo(location) > 30) {
                        writeJSONObject(location)  // Llama a la función para escribir en el archivo JSON
                    }
                    lastLocation = location  // Actualiza lastLocation con la nueva ubicación
                }
                if (lastLocation == null && result.locations.isNotEmpty()) {
                    lastLocation = result.locations.last()  // Si lastLocation era null, inicializa con la última ubicación
                }
            }
        }
    }



    private fun updateLocationUI(location: Location) {
        lastLocation = location  // Asegúrate de actualizar lastLocation con la nueva ubicación recibida.

        val latLng = LatLng(location.latitude, location.longitude)
        currentLocationMarker?.remove()
        currentLocationMarker = map.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Ubicación actual")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
        if (!primerZoom) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            primerZoom = true
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, locationPermissionName) == PackageManager.PERMISSION_GRANTED) {
            location.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun createLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        return locationRequest
    }
}
