package com.example.tallercompumovil2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

import android.location.Location
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.tallercompumovil2.databinding.ActivityMapsBinding
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, SensorEventListener {
    //mapa
    private lateinit var map: GoogleMap
    private val locationPermissionName = Manifest.permission.ACCESS_FINE_LOCATION
    private lateinit var location: FusedLocationProviderClient
    private lateinit var binding: ActivityMapsBinding
    private var currentLocationMarker: Marker? = null
    private var lastLocation: Location? = null

    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission(),
        ActivityResultCallback { isGranted ->
            if (isGranted) {
                startLocationUpdates()
            }
        })

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    //sensores
    private lateinit var sensorManager: SensorManager
    private var lightSensor: Sensor? = null
    private val umbralBajo = 50f

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
        } else {

        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (::map.isInitialized && event.sensor.type == Sensor.TYPE_LIGHT) { // Verifica si map está inicializado
            // Obtener el valor de la luminosidad
            val lightValue = event.values[0]

            // Cambiar el estilo del mapa según la luminosidad

            if (lightValue < umbralBajo) {
                // Aplicar estilo oscuro
                map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_night))
                val backgroundColor = ContextCompat.getColor(this, R.color.temaOscuro)
                val texto = ContextCompat.getColor(this, R.color.white)
                binding.activityMap.setBackgroundColor(backgroundColor)
                binding.altitude.setTextColor(texto)
                binding.latitude.setTextColor(texto)
                binding.longitude.setTextColor(texto)
            } else {
                // Aplicar estilo claro
                map.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.style_day))
                val backgroundColor = ContextCompat.getColor(this, R.color.white)
                val texto = ContextCompat.getColor(this, R.color.black)
                binding.activityMap.setBackgroundColor(backgroundColor)
                binding.altitude.setTextColor(texto)
                binding.latitude.setTextColor(texto)
                binding.longitude.setTextColor(texto)
            }

        }
    }

    //Es necesario crear este metodo para que funcione, mas no se debe implementar nada
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        startLocationUpdates()
    }

    private fun createLocationCallback(): LocationCallback {
        return object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                super.onLocationResult(result)
                val location = result.lastLocation ?: return // Si location es null, retorna temprano
                updateLocationUI(location)

                val lastLoc = lastLocation // Usa una copia local de lastLocation
                writeJsonFile(location)
                if (lastLoc == null || location.distanceTo(lastLoc) > 30) {
                    lastLocation = location
                    writeJsonFile(location)
                }
            }
        }
    }

    // actualizar la interfaz de usuario
    private fun updateLocationUI(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        currentLocationMarker?.remove() // Elimina el marcador de ubicación actual si ya existe.
        currentLocationMarker = map.addMarker(MarkerOptions().position(latLng).title("Ubicacion Actual"))
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f)) //centrar camara
        binding.latitude.text = getString(R.string.latitude_label) + ": ${location.latitude}"
        binding.longitude.text = getString(R.string.longitude_label) + ": ${location.longitude}"
        binding.altitude.text = getString(R.string.altitude_label) + ": ${location.altitude}"


    }
    //REVISAR ESTO, NO ENCUENTRO LOS ARCHIVOS LOCALES
    private fun writeJsonFile(location: Location) {
        val jsonObject = JSONObject()
        jsonObject.put("latitude", location.latitude)
        jsonObject.put("longitude", location.longitude)
        jsonObject.put("altitude", location.altitude)
        jsonObject.put("timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "locationUpdates.json")
        FileWriter(file, true).use { it.write(jsonObject.toString() + "\n") }
    }

    //revisa si se dio permiso, y empieza la ubicacion
    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, locationPermissionName) == PackageManager.PERMISSION_GRANTED) {
            location.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    private fun createLocationRequest(): LocationRequest {
        return LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000
            fastestInterval = 5000
        }
    }
}
