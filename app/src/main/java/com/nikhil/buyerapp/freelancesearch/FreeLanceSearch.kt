package com.nikhil.buyerapp.freelancesearch

import android.os.Bundle
import android.util.Log
import android.util.Log.e
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.FragmentFreeLanceSearchBinding
import com.nikhil.buyerapp.dataclasses.Freelancer
import com.nikhil.buyerapp.dataclasses.FreelancerItem

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FreeLanceSearch.newInstance] factory method to
 * create an instance of this fragment.
 */
class FreeLanceSearch : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var _binding: FragmentFreeLanceSearchBinding?=null
    val binding get()=_binding!!
    lateinit var primskill:String
    val db= Firebase.firestore
    var auth: FirebaseAuth = FirebaseAuth.getInstance()
    lateinit var freeadapter: FreelanceAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        primskill=arguments?.getString("categoryprimskill")!!
        Log.e("DEBUG","${primskill}")


        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding=FragmentFreeLanceSearchBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvresults.text="Results for ${primskill}"

        setuprecycler()
        fetchandmap()

    }
    fun fetchandmap() {
        db.collection("Freelancers").whereEqualTo("primaryskill", primskill).get()
            .addOnSuccessListener { snapshots ->
                Log.e("FIRESTORE", "Docs size = ${snapshots.size()}")
                val tempList = mutableListOf<FreelancerItem>()

                for (doc in snapshots.documents) {
                    if (doc.id == auth.currentUser?.uid) continue
                    val freelancer = doc.toObject(FreelancerItem::class.java)
                        ?.copy(uid = doc.id) ?: continue

                    db.collection("Users")
                        .document(doc.id) // SAME UID
                        .get()
                        .addOnSuccessListener { userDoc ->

                            val profileUrl = userDoc.getString("profilePictureUrl") ?: ""

                            val merged = freelancer.copy(
                                profileImageUrl = profileUrl
                            )

                            tempList.add(merged)
                            freeadapter.submitList(tempList.toList())


                        }
                }
            }
    }
    fun setuprecycler(){
        freeadapter= FreelanceAdapter (
            onclicked = { FreelancerItem ->

            val bundle= Bundle().apply {
                putString("uid",FreelancerItem.uid)
            }

            findNavController().navigate(
                R.id.scaffold,bundle
            )
                        },
            onContactClicked = { FreelancerItem ->
                val bundle = Bundle().apply {
                    putString("receiverUid", FreelancerItem.uid)
                    putString("receiverName", FreelancerItem.name)
                    putString("receiverImage", FreelancerItem.profileImageUrl)
                }
                findNavController().navigate(
                    R.id.chatlist,
                    bundle
                )
            }
        )
        binding.recyclerresults.apply {
            adapter=freeadapter
            layoutManager=LinearLayoutManager(requireContext())



        }

    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment FreeLanceSearch.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FreeLanceSearch().apply {
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