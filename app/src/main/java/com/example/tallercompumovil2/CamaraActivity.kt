package com.example.tallercompumovil2

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.tallercompumovil2.databinding.ActivityCamaraBinding
import java.io.File

class CamaraActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCamaraBinding

    private lateinit var uriCamera: Uri

    private val getContentGallery = registerForActivityResult(
        ActivityResultContracts.GetContent(),
        ActivityResultCallback { uri ->
            loadImage(uri)
        })

    private val getContentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
        ActivityResultCallback { success ->
            if (success) {
                loadImage(uriCamera)
            }
        })

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido
            val file = File(filesDir, "picFromCamera")
            uriCamera = FileProvider.getUriForFile(
                baseContext,
                "${baseContext.packageName}.fileprovider",
                file
            )
            getContentCamera.launch(uriCamera)
        } else {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamaraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Solicitar permiso al iniciar la actividad
        requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)

        binding.galeria.setOnClickListener {
            getContentGallery.launch("image/*")
        }

        binding.camara.setOnClickListener {
            getContentCamera.launch(uriCamera)
        }
    }

    private fun loadImage(uri: Uri?) {
        uri?.let {
            val imageStream = contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(imageStream)
            binding.imagenGaleria.setImageBitmap(bitmap)
        }
    }
}
