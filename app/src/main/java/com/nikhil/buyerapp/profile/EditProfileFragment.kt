package com.nikhil.buyerapp.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.nikhil.buyerapp.databinding.FragmentEditProfileBinding
import com.nikhil.buyerapp.dataclasses.Client
import com.nikhil.buyerapp.dataclasses.User
import com.nikhil.buyerapp.utils.snack

class EditProfileFragment : Fragment() {

    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()

        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        binding.imgbt.setOnClickListener {
            if (isAdded) {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun loadProfile() {

        val uid = auth.currentUser?.uid ?: return

        // USERS COLLECTION
        db.collection("Users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                if (_binding == null || !isAdded) {
                    return@addOnSuccessListener
                }

                val user = document.toObject(User::class.java)

                binding.etname.setText(user?.fullName ?: "")
                binding.etPhone.setText(user?.phoneNumber ?: "")
                binding.etemail.setText(user?.email ?: "")
                binding.etdesc.setText(user?.bio ?: "")
            }
            .addOnFailureListener {
                if (_binding == null || !isAdded) return@addOnFailureListener
                snack("Failed to load profile")
            }

        // CLIENT COLLECTION
        db.collection("Client")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                if (_binding == null || !isAdded) {
                    return@addOnSuccessListener
                }

                val client = document.toObject(Client::class.java)

                binding.etprim.setText(client?.companyName ?: "")
            }
            .addOnFailureListener {
                if (_binding == null || !isAdded) return@addOnFailureListener
                snack("Failed to load company info")
            }
    }

    private fun saveProfile() {

        val uid = auth.currentUser?.uid ?: return

        val fullName = binding.etname.text.toString().trim()
        val bio = binding.etdesc.text.toString().trim()
        val companyName = binding.etprim.text.toString().trim()

        binding.btnSave.isEnabled = false

        // ATOMIC WRITE: both updates succeed or both fail, no partial save
        val userRef = db.collection("Users").document(uid)
        val clientRef = db.collection("Client").document(uid)

        db.runBatch { batch ->
            batch.update(
                userRef,
                mapOf(
                    "fullName" to fullName,
                    "bio" to bio
                )
            )
            batch.update(
                clientRef,
                mapOf(
                    "companyName" to companyName
                )
            )
        }
            .addOnSuccessListener {
                if (_binding == null || !isAdded) return@addOnSuccessListener
                binding.btnSave.isEnabled = true
                snack("Profile Updated")
            }
            .addOnFailureListener {
                if (_binding == null || !isAdded) return@addOnFailureListener
                binding.btnSave.isEnabled = true
                snack("Failed to update profile")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}