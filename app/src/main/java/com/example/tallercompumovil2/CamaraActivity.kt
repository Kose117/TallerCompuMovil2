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

    val getContentGallery = registerForActivityResult(
        ActivityResultContracts.GetContent(),
        ActivityResultCallback {
            loadImage(it)
        })

    val getContentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture(),
        ActivityResultCallback {
            if(it){
                loadImage(uriCamera)
            }
        })

    private lateinit var uriCamera : Uri
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCamaraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val file = File(getFilesDir(), "picFromCamera");
        uriCamera = FileProvider.getUriForFile(baseContext,baseContext.packageName + ".fileprovider", file)

        binding.galeria.setOnClickListener {
            getContentGallery.launch("image/*")
        }

        binding.camara.setOnClickListener {
            getContentCamera.launch(uriCamera)
        }

    }


    private fun loadImage(uri : Uri?) {
        val imageStream = getContentResolver().openInputStream(uri!!)
        val bitmap = BitmapFactory.decodeStream(imageStream)
        binding.imagenGaleria.setImageBitmap(bitmap)
    }
}