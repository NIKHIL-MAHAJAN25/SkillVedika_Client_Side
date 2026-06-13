package com.nikhil.buyerapp.comprofile

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.ActivityProfileScreen1Binding
import com.nikhil.buyerapp.dataclasses.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.uploadAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileScreen1 : AppCompatActivity() {
    lateinit var binding: ActivityProfileScreen1Binding
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    val db = Firebase.firestore

    private lateinit var supabaseClient: SupabaseClient
    private val pickImageLauncher =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->

            if (uri != null) {
                uploadImageToSupabase(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileScreen1Binding.inflate(layoutInflater)
        setContentView(binding.root)
        val occupationlist = listOf(
            "Business Owner",
            "Salaried Employee",
            "Freelancer/self Employed",
            "Student",
            "Not applicable"
        )
        val statesList = listOf(
            "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
            "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand",
            "Karnataka", "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur",
            "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
            "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura",
            "Uttar Pradesh", "Uttarakhand", "West Bengal", "Andaman and Nicobar Islands",
            "Chandigarh", "Dadra and Nagar Haveli and Daman and Diu", "Delhi",
            "Jammu and Kashmir", "Ladakh", "Lakshadweep", "Puducherry"
        )

        val aradapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statesList)
        binding.actState.setAdapter(aradapter)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val ocadapter = ArrayAdapter(this, R.layout.item_dropdown_interest, occupationlist)
        binding.etoccup2.setAdapter(ocadapter)
        binding.etoccup2.setOnClickListener {
            binding.etoccup2.showDropDown()
        }

        supabaseClient = (this.application as supabasefile).supabaseClient
        binding.cameraFab.setOnClickListener {

            pickImageLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }
        binding.btnNext.setOnClickListener {


            val uid = auth.currentUser?.uid
            val aname = binding.etname2.text.toString()
            val code = binding.countryCodePicker.selectedCountryCodeWithPlus
            val numbere = binding.etPhone.text.toString()
            val full = "$code$numbere"
            val occupations = binding.etoccup2.text.toString()
            val states = binding.actState.text.toString()

            val userUpdates = mapOf(
                "fullName" to aname,
                "phoneNumber" to full,
                "occupation" to occupations,
                "state" to states


            )
            if (uid != null) {
                db.collection("Users").document(uid).update(userUpdates).addOnSuccessListener {
                    Toast.makeText(this, "Data saved", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, ProfileScreen2::class.java))
                }
            }

        }

    }







        private fun uploadImageToSupabase(uri: Uri) {

            val byteArray = uriToByteArray(this, uri)

            if (byteArray.size > 5 * 1024 * 1024) {

                Toast.makeText(
                    this,
                    "Image must be under 5 MB",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            binding.cameraFab.isEnabled = false
            val fileName = "uploads/${System.currentTimeMillis()}.jpg"

            val bucket = supabaseClient.storage.from("sample") // Choose your bucket name

            // Use lifecycleScope for safe coroutine usage
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Upload image and handle the response
                    bucket.uploadAsFlow(fileName, byteArray).collect { status ->
                        withContext(Dispatchers.Main) {
                            when (status) {
                                is UploadStatus.Progress -> {
//                                val progress = (status.totalBytesSent.toFloat() / status.contentLength * 100)
                                    Log.d("Upload", "Progress%")
                                }

                                is UploadStatus.Success -> {

                                    binding.cameraFab.isEnabled = true

                                    Log.d("Upload ", "Upload Success")

                                    handleUploadSuccess(bucket, fileName)
                                }

                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        binding.cameraFab.isEnabled = true

                        Log.e("Upload", "Error uploading image: ${e.message}")

                    }
                }
            }
        }

    private fun uriToByteArray(
        context: Context,
        uri: Uri
    ): ByteArray {

        return context.contentResolver
            .openInputStream(uri)
            ?.use {
                it.readBytes()
            }
            ?: throw Exception("Unable to read image")
    }





    private fun handleUploadSuccess(bucket: Any, fileName: String) {
        try {
            val imageUrl = supabaseClient.storage.from("sample").publicUrl(fileName)
            Log.d("ProfileFragment", "Generated public URL: $imageUrl")

            val currentUser = auth.currentUser
            if (currentUser != null) {
                Log.d("ProfileFragment", "Updating Firestore with new image URL")
                db.collection("Users")
                    .document(currentUser.uid)
                    .update("profilePictureUrl",imageUrl)
                    .addOnSuccessListener {
                        Log.d("ProfileFragment", "Firestore update successful")
                        Toast.makeText(this, "Profile image updated!", Toast.LENGTH_SHORT).show()
                        Glide.with(this)
                            .load(imageUrl)
                            .error(R.drawable.ic_launcher_background)
                            .into(binding.profileImage)
                        }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreUpdate", "Failed to update profile image URL: ${e.message}", e)
                        Toast.makeText(this, "Failed to update profile image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Log.e("ProfileFragment", "Current user is null")
                Toast.makeText(this, "User authentication error", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error in handleUploadSuccess: ${e.message}", e)
            Toast.makeText(this, "Error processing upload success: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}