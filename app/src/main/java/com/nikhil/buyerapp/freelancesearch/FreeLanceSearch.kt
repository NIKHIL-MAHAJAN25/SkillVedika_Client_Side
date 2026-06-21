package com.nikhil.buyerapp.freelancesearch

import android.os.Bundle
import android.util.Log
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
import com.nikhil.buyerapp.dataclasses.FreelancerItem

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FreeLanceSearch.newInstance] factory method to
 * create an instance of this fragment.
 */
class FreeLanceSearch : Fragment() {

    private var param1: String? = null
    private var param2: String? = null

    private var _binding: FragmentFreeLanceSearchBinding? = null
    val binding get() = _binding!!

    lateinit var primskill: String
    val db = Firebase.firestore
    var auth: FirebaseAuth = FirebaseAuth.getInstance()
    lateinit var freeadapter: FreelanceAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        primskill = arguments?.getString("categoryprimskill") ?: ""
        Log.e("DEBUG", primskill)

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFreeLanceSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.shimmerLayout.startShimmer()
        binding.shimmerLayout.visibility = View.VISIBLE
        binding.recyclerresults.visibility = View.GONE
        binding.tvresults.text = "Results for $primskill"

        setuprecycler()
        fetchandmap()
    }

    private fun stopShimmerAndShow() {
        if (_binding == null) return
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = View.GONE
        binding.recyclerresults.visibility = View.VISIBLE
    }

    fun fetchandmap() {

        db.collection("Freelancers")
            .whereEqualTo("primaryskill", primskill)
            .get()
            .addOnSuccessListener { snapshots ->

                if (_binding == null || !isAdded) return@addOnSuccessListener

                Log.e("FIRESTORE", "Docs size = ${snapshots.size()}")

                val otherDocs = snapshots.documents.filter { it.id != auth.currentUser?.uid }

                if (otherDocs.isEmpty()) {
                    freeadapter.submitList(emptyList())
                    stopShimmerAndShow()
                    return@addOnSuccessListener
                }

                val tempList = mutableListOf<FreelancerItem>()
                var loadedCount = 0
                val totalCount = otherDocs.size

                fun onOneLoaded() {
                    loadedCount++
                    if (loadedCount == totalCount) {
                        if (_binding == null || !isAdded) return
                        freeadapter.submitList(tempList.toList())
                        stopShimmerAndShow()
                    }
                }

                for (doc in otherDocs) {

                    val freelancer = doc.toObject(FreelancerItem::class.java)
                        ?.copy(uid = doc.id)

                    if (freelancer == null) {
                        // still counts toward completion, or loadedCount never reaches totalCount
                        onOneLoaded()
                        continue
                    }

                    db.collection("Users")
                        .document(doc.id)
                        .get()
                        .addOnSuccessListener { userDoc ->

                            if (_binding == null || !isAdded) return@addOnSuccessListener

                            val profileUrl = userDoc.getString("profilePictureUrl") ?: ""

                            tempList.add(
                                freelancer.copy(profileImageUrl = profileUrl)
                            )

                            onOneLoaded()
                        }
                        .addOnFailureListener {
                            if (_binding == null || !isAdded) return@addOnFailureListener
                            onOneLoaded()
                        }
                }
            }
            .addOnFailureListener {
                stopShimmerAndShow()
            }
    }

    fun setuprecycler() {
        freeadapter = FreelanceAdapter(
            onclicked = { freelancerItem ->
                val bundle = Bundle().apply {
                    putString("uid", freelancerItem.uid)
                }
                findNavController().navigate(R.id.scaffold, bundle)
            },
            onContactClicked = { freelancerItem ->
                val bundle = Bundle().apply {
                    putString("receiverUid", freelancerItem.uid)
                    putString("receiverName", freelancerItem.name)
                    putString("receiverImage", freelancerItem.profileImageUrl)
                }
                findNavController().navigate(R.id.chatlist, bundle)
            }
        )
        binding.recyclerresults.apply {
            adapter = freeadapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    companion object {
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
        _binding?.shimmerLayout?.stopShimmer()
        _binding = null
        super.onDestroyView()
    }
}