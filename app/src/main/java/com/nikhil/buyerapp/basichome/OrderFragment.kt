package com.nikhil.buyerapp.basichome

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.FragmentOrderBinding
import com.nikhil.buyerapp.dataclasses.Project
import com.nikhil.buyerapp.displayingorders.OrderAdapter

class OrderFragment : Fragment() {

    private var _binding: FragmentOrderBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: OrderAdapter
    private var allProjects = listOf<Project>()

    private val db = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        setupTabFilter()
        loadProjects()

        binding.addpost.setOnClickListener {
            findNavController().navigate(R.id.action_post)
        }
    }

    private fun setupRecycler() {
        adapter = OrderAdapter { project ->
            Log.d("CLICKED_PROJECT", project.title)
        }
        binding.projectrecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.projectrecycler.isNestedScrollingEnabled = false
        binding.projectrecycler.setHasFixedSize(false)
        binding.projectrecycler.adapter = adapter
    }

    private fun setupTabFilter() {
        binding.tabLayoutStatus.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                applyFilter(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun applyFilter(tabPosition: Int) {
        val status = when (tabPosition) {
            0 -> "OPEN"
            1 -> "ASSIGNED"
            2 -> "COMPLETED"
            3 -> "CANCELLED"
            else -> "OPEN"
        }
        adapter.submitList(allProjects.filter { it.status == status })
    }

    private fun loadProjects() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("AUTH", "User not logged in")
            return
        }

        db.collection("Projects")
            .whereEqualTo("clientuid", uid)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("FIRESTORE_ERROR", error.message.toString())
                    return@addSnapshotListener
                }

                val projectList = mutableListOf<Project>()
                snapshots?.documents?.forEach { document ->
                    val project = document.toObject(Project::class.java)
                    if (project != null) {
                        project.projectid = document.id
                        projectList.add(project)
                    }
                }

                allProjects = projectList
                applyFilter(binding.tabLayoutStatus.selectedTabPosition)

                Log.d("FIRESTORE_DATA", "Projects loaded: ${projectList.size}")
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}