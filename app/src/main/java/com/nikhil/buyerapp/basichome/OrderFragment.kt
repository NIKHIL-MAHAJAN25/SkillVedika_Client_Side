package com.nikhil.buyerapp.basichome

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.DialogAssignFreelancerBinding
import com.nikhil.buyerapp.databinding.FragmentOrderBinding
import com.nikhil.buyerapp.dataclasses.Project
import com.nikhil.buyerapp.dataclasses.ProjectStatus
import com.nikhil.buyerapp.displayingorders.OrderAdapter

class OrderFragment : Fragment() {

    private var _binding: FragmentOrderBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: OrderAdapter
    private var allProjects = listOf<Project>()

    private val db = FirebaseFirestore.getInstance()
    private var firestoreListener: ListenerRegistration? = null
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
        adapter = OrderAdapter(
            onClicked = { project ->
                Log.d("CLICKED_PROJECT", project.title)
            },
            onAssignFreelancer = { project ->
                showAssignDialog(project)
            },
            onMarkCompleted = { project ->
                confirmAndUpdate(
                    title = "Mark as Completed?",
                    message = "This will mark \"${project.title}\" as completed.",
                    project = project,
                    newStatus = ProjectStatus.COMPLETED.name,
                    extraFields = mapOf("completedAt" to FieldValue.serverTimestamp())
                )
            },
            onCancelProject = { project ->
                confirmAndUpdate(
                    title = "Cancel Project?",
                    message = "Are you sure you want to cancel \"${project.title}\"?",
                    project = project,
                    newStatus = ProjectStatus.CANCELLED.name
                )
            }
        )
        binding.projectrecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.projectrecycler.isNestedScrollingEnabled = false
        binding.projectrecycler.adapter = adapter
    }

    private fun showAssignDialog(project: Project) {
        val dialogBinding = DialogAssignFreelancerBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        // change hint to email in your dialog XML too
        dialogBinding.etFreelancerUid.hint = "Freelancer Email"

        dialogBinding.btnAssign.setOnClickListener {
            val name = dialogBinding.etFreelancerName.text.toString().trim()
            val email = dialogBinding.etFreelancerUid.text.toString().trim()

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "Fill in both fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // look up UID from Users collection by email
            db.collection("Users")
                .whereEqualTo("email", email)
                .limit(1)
                .get()
                .addOnSuccessListener { userDocs ->
                    if (userDocs.isEmpty) {
                        Toast.makeText(requireContext(), "No freelancer found with that email", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val actualUid = userDocs.documents[0].id  // document ID is the UID

                    db.collection("Projects").document(project.projectid)
                        .update(
                            "status", ProjectStatus.ASSIGNED.name,
                            "freeuid", actualUid,          // real UID now
                            "freename", name,
                            "freeemail", email,            // store email separately too
                            "startedAt", FieldValue.serverTimestamp()
                        )
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Freelancer assigned!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Log.e("ASSIGN", "Failed to assign", e)
                            Toast.makeText(requireContext(), "Failed to assign", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("ASSIGN", "User lookup failed", e)
                    Toast.makeText(requireContext(), "Lookup failed", Toast.LENGTH_SHORT).show()
                }
        }

        dialog.show()
    }

    private fun confirmAndUpdate(
        title: String,
        message: String,
        project: Project,
        newStatus: String,
        extraFields: Map<String, Any> = emptyMap()
    ) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ ->
                val updates = mutableMapOf<String, Any>("status" to newStatus)
                updates.putAll(extraFields)
                db.collection("Projects").document(project.projectid)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Updated!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("UPDATE", "Failed", e)
                        Toast.makeText(requireContext(), "Failed to update", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("No", null)
            .show()
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
            0 -> ProjectStatus.OPEN.name
            1 -> ProjectStatus.ASSIGNED.name
            2 -> ProjectStatus.COMPLETED.name
            3 -> ProjectStatus.CANCELLED.name
            else -> ProjectStatus.OPEN.name
        }
        adapter.submitList(allProjects.filter { it.status == status })
    }

    private fun loadProjects() {
        val uid = auth.currentUser?.uid ?: run {
            Log.e("AUTH", "User not logged in")
            return
        }

        firestoreListener = db.collection("Projects")
            .whereEqualTo("clientuid", uid)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("FIRESTORE_ERROR", error.message.toString())
                    return@addSnapshotListener
                }
                if (_binding == null) return@addSnapshotListener

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
        firestoreListener?.remove()
        _binding = null
    }
}