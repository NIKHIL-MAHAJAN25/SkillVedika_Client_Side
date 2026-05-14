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

        _binding =
            FragmentEditProfileBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()

        binding.btnSave.setOnClickListener {

            saveProfile()
        }

        binding.imgbt.setOnClickListener {

            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadProfile() {

        val uid = auth.currentUser?.uid ?: return

        // USERS COLLECTION
        db.collection("Users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                val user =
                    document.toObject(User::class.java)

                binding.etname.setText(
                    user?.fullName ?: ""
                )

                binding.etPhone.setText(
                    user?.phoneNumber ?: ""
                )

                binding.etemail.setText(
                    user?.email ?: ""
                )

                binding.etdesc.setText(
                    user?.bio ?: ""
                )
            }

        // CLIENT COLLECTION
        db.collection("Client")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                val client =
                    document.toObject(Client::class.java)

                binding.etprim.setText(
                    client?.companyName ?: ""
                )
            }
    }

    private fun saveProfile() {

        val uid = auth.currentUser?.uid ?: return

        val fullName =
            binding.etname.text.toString().trim()

        val bio =
            binding.etdesc.text.toString().trim()

        val companyName =
            binding.etprim.text.toString().trim()

        // UPDATE USERS COLLECTION
        db.collection("Users")
            .document(uid)
            .update(
                mapOf(
                    "fullName" to fullName,
                    "bio" to bio
                )
            )

        // UPDATE CLIENT COLLECTION
        db.collection("Client")
            .document(uid)
            .update(
                mapOf(
                    "companyName" to companyName
                )
            )
            .addOnSuccessListener {

                snack("Profile Updated")
            }
            .addOnFailureListener {

                snack("Failed to update profile")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}