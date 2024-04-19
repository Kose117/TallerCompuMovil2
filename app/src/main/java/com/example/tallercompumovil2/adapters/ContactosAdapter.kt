package com.example.tallercompumovil2.adapters

import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CursorAdapter
import com.example.tallercompumovil2.databinding.ContactrowBinding

class ContactosAdapter(context: Context?, c: Cursor?, flags: Int) :
    CursorAdapter(context, c, flags) {

    override fun newView(context: Context?, cursor: Cursor?, parent: ViewGroup?): View {
        val inflater = LayoutInflater.from(context)
        val binding = ContactrowBinding.inflate(inflater, parent, false)
        return binding.root
    }

    override fun bindView(view: View, context: Context?, cursor: Cursor?) {
        val binding = ContactrowBinding.bind(view)
        val idIndex = cursor?.getColumnIndex(ContactsContract.Profile._ID) ?: -1
        val nameIndex = cursor?.getColumnIndex(ContactsContract.Profile.DISPLAY_NAME_PRIMARY) ?: -1

        if (idIndex != -1 && nameIndex != -1 && cursor != null) {
            val contactId = cursor.getInt(idIndex)
            val contactName = cursor.getString(nameIndex)

            binding.contactId.text = contactId.toString()
            binding.contactName.text = contactName
        } else {
            binding.contactId.text = "No ID"
            binding.contactName.text = "No Name"
        }
    }

    fun clear() {
        swapCursor(null) // Use swapCursor to clear the adapter's data safely
    }
}
