package com.nikhil.buyerapp.freelanceprofileview

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.ktx.Firebase
import com.nikhil.buyerapp.MainActivity
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.FragmentScaffoldBinding
import com.nikhil.buyerapp.dataclasses.Freelancer
import com.nikhil.buyerapp.dataclasses.User

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"




/**
 * A simple [Fragment] subclass.
 * Use the [ScaffoldFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ScaffoldFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var _binding: FragmentScaffoldBinding?=null
    private val binding get()=_binding!!
    val db= Firebase.firestore
    lateinit var uid: String
    private val auth: FirebaseAuth=FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uid=arguments?.getString("uid")!!
        Log.e("DEBUG","${uid}")
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding=FragmentScaffoldBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadinfo()
        loadotherinfo()

        binding.chipskills.setOnCheckedStateChangeListener{group,checkedIds->
            val checkedId=checkedIds.firstOrNull()?:return@setOnCheckedStateChangeListener
            when (checkedId) {
                R.id.basichip -> replaceFragment(BasicFragment())
                R.id.skills -> replaceFragment(SkillsFragment())
                R.id.exp -> replaceFragment(ExperienceFragment())
            }
        }
        if (savedInstanceState == null) {
            binding.basichip.isChecked = true
        }
    }



    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ScaffoldFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ScaffoldFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
    private fun loadotherinfo() {
        if (uid != null) {
            db.collection("Freelancers").document(uid)
                .addSnapshotListener { snapshot, error ->
                    //Now what happens is we are getting npe exception ie firestore add on listener run asyncronoulsy and giving result even when fragment is destroyed so we make a local
                    //copy of binding
                    val b = _binding ?: return@addSnapshotListener
                    if (error != null) {
                        // Handle error, maybe log it
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val user = snapshot.toObject<Freelancer>()
                        b.tvtitle.setText(user?.primaryskill)
                        val rate = user?.projectRate ?: 0.0
                        b.tvrate.text = "₹$rate/hour"
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
                        b.tvname.text=user?.fullName
                        Glide.with(this@ScaffoldFragment)
                            .load(user?.profilePictureUrl)
                            .error(R.drawable.ic_launcher_background)
                            .into(binding.profileImage)



                    }
                }
        }
    }
        private fun replaceFragment(fragment: Fragment) {
            fragment.arguments = Bundle().apply {
                putString("uid", uid)
            }
            childFragmentManager.beginTransaction()
                .replace(R.id.framelayout, fragment)
                .commit()
//            // 1. Get the specialized manager for nested fragments.
//            val fragmentManager = childFragmentManager
//            // 2. Start a transaction.
//            val transaction = fragmentManager.beginTransaction()
//            // 3. Replace the content of the container with the new fragment.
//            transaction.replace(R.id.framelayout, fragment)
//            // 4. Commit the transaction to make it happen.
//            transaction.commit()
        }

        override fun onDestroyView() {
            super.onDestroyView()
            _binding = null
        }
    }