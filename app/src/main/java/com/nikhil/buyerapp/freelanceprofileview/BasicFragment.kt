package com.nikhil.buyerapp.freelanceprofileview

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.FragmentBasicBinding
import com.nikhil.buyerapp.dataclasses.Certification
import com.nikhil.buyerapp.dataclasses.Freelancer
import com.nikhil.buyerapp.dataclasses.Qualification

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BasicFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BasicFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var _binding: FragmentBasicBinding?=null
    private val binding get()=_binding!!
    private val db=Firebase.firestore
    private var flag: Boolean =false
    var userlist= arrayListOf<Certification>()
    val qlist= arrayListOf<Qualification>()
    lateinit var qadapter:QualAdapter
    private lateinit var reviewAdapter: ReviewAdapter

    lateinit var uid:String
    lateinit var adapterr: CertAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uid=arguments?.getString("uid")!!
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
        _binding=FragmentBasicBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapterr= CertAdapter(userlist)
        binding.recylercert.layoutManager=LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
        binding.recylercert.adapter=adapterr
        userlist.clear()
        setupReviewRecycler()





        qadapter= QualAdapter(qlist)
        binding.recylerqual.layoutManager=LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.recylerqual.adapter=qadapter
        binding.addReview.setOnClickListener {
            val bundle = Bundle().apply {
                putString("uid",uid)
            }
            findNavController().navigate(R.id.review,bundle)
        }

        qlist.clear()
        startlisten()
        startlistencert()
        startReviewListener()
    }
    private fun startReviewListener() {

        db.collection("Freelancers")
            .document(uid)

            .addSnapshotListener { snapshot, error ->

                if (error != null) {

                    Log.e(
                        "Firestore Review",
                        "Review listener failed",
                        error
                    )

                    return@addSnapshotListener
                }

                if (_binding == null) return@addSnapshotListener

                if (snapshot != null && snapshot.exists()) {

                    val freelancer =
                        snapshot.toObject(Freelancer::class.java)

                    // RATING
                    val rating =
                        freelancer?.rating ?: 0.0

                    if (rating > 0) {

                        binding.tvRating.text =
                            "⭐ %.1f".format(rating)

                    } else {

                        binding.tvRating.text =
                            "No ratings"
                    }

                    // REVIEWS
                    val reviews =
                        freelancer?.reviews ?: emptyList()

                    if (reviews.isNotEmpty()) {

                        val sortedReviews =
                            reviews.sortedByDescending {
                                it.timestamp
                            }

                        reviewAdapter.submitList(
                            sortedReviews
                        )

                    } else {

                        reviewAdapter.submitList(
                            emptyList()
                        )
                    }

                } else {

                    Log.d(
                        "Firestore Review",
                        "Freelancer document missing"
                    )
                }
            }
    }
    private fun setupReviewRecycler() {
        reviewAdapter = ReviewAdapter()
        binding.recylerReview.apply {
            adapter = reviewAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL,false)
            isNestedScrollingEnabled = false
        }
    }
    private fun startlistencert(){
        if(uid!=null){
            db.collection("Freelancers").document(uid).addSnapshotListener{snapshot,error->
                if(error!=null){
                    Log.e("Firestore error","Listen failed",error)
                    return@addSnapshotListener
                }
                if(snapshot!=null && snapshot.exists()){
                    val freelancer=snapshot.toObject(Freelancer::class.java)
                    val newcert=freelancer?.certification?: emptyList()
                    adapterr.updateData(newcert)
                }else{
                    Log.d("Firestore Info", "Current data: null")

                }
            }
        }
    }
    private fun startlisten(){
        if(uid!=null){
            db.collection("Freelancers").document(uid).addSnapshotListener{snapshot,error->
                if(error!=null){
                    Log.e("Firestore error","Listen failed",error)
                    return@addSnapshotListener
                }
                if(snapshot!=null && snapshot.exists()){
                    val freelancer=snapshot.toObject(Freelancer::class.java)
                    val newqual=freelancer?.qualification?: emptyList()
                    qadapter.updateData(newqual)
                }else{
                    Log.d("Firestore Info", "Current data: null")

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
         * @return A new instance of fragment BasicFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BasicFragment().apply {
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