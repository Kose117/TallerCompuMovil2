package com.example.tallercompumovil2

import android.content.pm.PackageManager

import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.tallercompumovil2.adapters.ContactosAdapter
import com.example.tallercompumovil2.databinding.ActivityContactosBinding

class ContactosActivity : AppCompatActivity() {

    val getContactsPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
        ActivityResultCallback {
            updateUI(it)
        })

    private lateinit var adapter : ContactosAdapter
    private lateinit var binding: ActivityContactosBinding
    val projection = arrayOf(ContactsContract.Profile._ID, ContactsContract.Profile.DISPLAY_NAME_PRIMARY)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = ContactosAdapter(this, null, 0)
        binding.listaContactos.adapter = adapter

        permissionRequest()
    }

    fun permissionRequest(){

        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED){
            if(shouldShowRequestPermissionRationale(android.Manifest.permission.READ_CONTACTS)){
                Toast.makeText(this, "The app requires access to the contacts", Toast.LENGTH_LONG).show()
            }
            getContactsPermission.launch(android.Manifest.permission.READ_CONTACTS)
        }else{
            updateUI(true)
        }
    }

    fun updateUI(contacts : Boolean){
        if(contacts){
            val cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, projection, null, null, null)
            if (cursor != null) {
                adapter.swapCursor(cursor) // Use swapCursor here
            } else {
                Toast.makeText(this, "Failed to load contacts", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Permission Denied
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            adapter.clear()
        }
    }






}
