package com.example.tallercompumovil2.adapters

import android.content.Context
import android.database.Cursor
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter

import com.example.tallercompumovil2.databinding.ContactrowBinding

class ContactosAdapter(context: Context?, c: Cursor?, flags: Int) :
    CursorAdapter(context, c, flags) {
        private lateinit var binding: ContactrowBinding

    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        binding = ContactrowBinding.inflate(LayoutInflater.from(context))
        println("controlador")
        return binding.row
    }

    override fun bindView(view: View?, context: Context?, cursor: Cursor?) {
        val contactId = cursor?.getInt(0)
        val contactName = cursor?.getString(1)
        binding.contactId.text = contactId.toString()
        binding.contactName.text = contactName
    }


}