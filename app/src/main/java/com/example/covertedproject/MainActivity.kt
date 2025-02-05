package com.example.covertedproject

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.covertedproject.ui.theme.CovertedProjectTheme
import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.material.snackbar.Snackbar

private val REQUEST_READ_CONTACTS: Int = 1231

private val contacts = mutableListOf<MainActivity.ContactInfo>()

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CovertedProjectTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ListContacts()
                }
            }
        }
        //Initialize Broadcast
        val filter = IntentFilter()
        filter.addAction("com.example.maincomponents.MainBroadcastReceiver")
        registerReceiver(MainBroadcastReceiver(), filter, RECEIVER_EXPORTED)

        val intent = Intent("com.example.maincomponents.MainBroadcastReceiver")
        sendBroadcast(intent)

        loadContacts()
    }

    @Composable
    fun ListContacts(
        modifier: Modifier = Modifier
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = modifier
        ) {
            items(contacts) { contact ->
                ContactItemView(contact = contact)
            }

        }
    }

    @Composable
    fun ContactItemView(contact: ContactInfo) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.Blue,
                contentColor = Color.White
            )

        ) {
            Column {
                Text(text = "ID: ${contact.id}")
                Text(text = "Name: ${contact.name}")
                Text(text = "Phone: ${contact.phone}")
                Text(text = "Email: ${contact.email}")
            }
        }
    }

    private fun loadContacts() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val contacts = getContactList(this)
            Log.d("Contacts", contacts.joinToString(separator = "\n"))
//            Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
        } else {
            requestContactsPermission();
            Toast.makeText(this, "Permission!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestContactsPermission() {
        // Check if the permission has already been granted
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission has already been granted, do something with the contact list
            val contacts = getContactList(this)
            Log.d("Contacts", contacts.joinToString(separator = "\n"))
            Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
            // Do something with the contact list
        } else {
            // Permission has not been granted, request it
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_CONTACTS
                )
            ) {
                // Explain why the app needs the permission
                // You can show a dialog or a Snackbar here
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "The app needs permission to access your contacts.",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("OK") {
                    // Request the permission
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_CONTACTS),
                        REQUEST_READ_CONTACTS
                    )
                }.show()
            } else {
                // Request the permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_CONTACTS),
                    REQUEST_READ_CONTACTS
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_CONTACTS && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission has been granted, do something with the contact list
            val contacts = getContactList(this)
            Log.d("Contacts", contacts.joinToString(separator = "\n"))
            Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show()
            // Do something with the contact list
        } else {
            // Permission has been denied
            // You can show a dialog or a Snackbar here to explain why the app needs the permission
        }
    }

    @SuppressLint("Range")
    fun getContactList(context: Context): List<ContactInfo> {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            null, null, null, null
        )
        cursor?.use {
            while (it.moveToNext()) {
                val id = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                val name = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                val hasPhoneNumber =
                    it.getInt(it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0
                if (hasPhoneNumber) {
                    val phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(id),
                        null
                    )
                    var email: String? = null
                    val emailCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                        arrayOf(id), null
                    )
                    emailCursor?.use { ec ->
                        if (ec.moveToNext()) {
                            email = ec.getString(ec.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS))
                        }
                    }
                    phoneCursor?.use { pc ->
                        while (pc.moveToNext()) {
                            val phoneNumber =
                                pc.getString(pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            val id =
                                pc.getString(pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                            val contact = ContactInfo(name, phoneNumber, id, email)
                            contacts.add(contact)
                        }
                    }
                    phoneCursor?.close()
                } else {
                    val contact = ContactInfo(name, null, null, null)
                    contacts.add(contact)
                }
            }
        }
        cursor?.close()
        return contacts
    }
    data class ContactInfo(
        val name: String?,
        val phone: String?,
        val id: String?,
        val email: String?
    )
}
