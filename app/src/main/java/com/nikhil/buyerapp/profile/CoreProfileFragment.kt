package com.nikhil.buyerapp.profile

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.nikhil.buyerapp.Login.LoginActivity
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.FragmentProfileBinding
import com.nikhil.buyerapp.dataclasses.Client
import com.nikhil.buyerapp.dataclasses.User

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CoreProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CoreProfileFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private  var _binding: FragmentProfileBinding?=null
    private val binding get()=_binding!!
    private val auth: FirebaseAuth= FirebaseAuth.getInstance()
    val uid=auth.currentUser?.uid
    private val db= Firebase.firestore



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding=FragmentProfileBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadinfo()
        loadotherinfo()
        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_prof_to_edit)
        }
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent= Intent(requireContext(), LoginActivity::class.java)
            intent.flags=Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()


        }
    }
    private fun loadotherinfo() {
        if (uid != null) {
            db.collection("Client").document(uid)
                .addSnapshotListener { snapshot, error ->
                    //Now what happens is we are getting npe exception ie firestore add on listener run asyncronoulsy and giving result even when fragment is destroyed so we make a local
                    //copy of binding
                    val b = _binding ?: return@addSnapshotListener
                    if (error != null) {
                        // Handle error, maybe log it
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val user = snapshot.toObject<Client>()
                        b.tvName.setText(user?.name)
                        b.tvCompanyName.setText(user?.companyName)
                    }
                }
        }
    }
    private fun loadinfo(){
        if(uid!=null){
            db.collection("Users").document(uid)
                .addSnapshotListener { snapshot,error->
                    //Now what happens is we are getting npe exception ie firestore add on listener run asyncronoulsy and giving result even when fragment is destroyed so we make a local
                    //copy of binding
                    val b=_binding?:return@addSnapshotListener
                    if (error != null) {
                        // Handle error, maybe log it
                        return@addSnapshotListener
                    }
                    if(snapshot != null && snapshot.exists()){
                        val user=snapshot.toObject<User>()

                        Glide.with(this@CoreProfileFragment)
                            .load(user?.profilePictureUrl)
                            .error(R.drawable.ic_launcher_background)
                            .into(binding.profileImage)



                    }
                }
        }
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CoreProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }
}