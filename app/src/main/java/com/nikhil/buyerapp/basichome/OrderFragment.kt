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
import com.nikhil.buyerapp.databinding.DialogAssignFromChatsBinding
import com.nikhil.buyerapp.databinding.FragmentOrderBinding
import com.nikhil.buyerapp.dataclasses.Project
import com.nikhil.buyerapp.dataclasses.ProjectStatus
import com.nikhil.buyerapp.displayingorders.FreelancerItem
import com.nikhil.buyerapp.displayingorders.FreelancerSelectAdapter
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
        if (!isAdded) return
        val dialogBinding = DialogAssignFromChatsBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.show()

        val uid = auth.currentUser?.uid ?: run {
            dialog.dismiss()
            return
        }

        db.collection("Chat")
            .whereArrayContains("participants", uid)
            .get()
            .addOnSuccessListener { chatDocs ->
                if (_binding == null || !isAdded) {
                    dialog.dismiss()
                    return@addOnSuccessListener
                }

                if (chatDocs.isEmpty) {
                    dialogBinding.progressBar.visibility = View.GONE
                    dialogBinding.tvEmpty.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                // Unique UIDs of everyone this buyer has chatted with
                val otherUids = chatDocs.documents
                    .mapNotNull { doc ->
                        val participants = doc.get("participants") as? List<*>
                        participants?.filterIsInstance<String>()?.firstOrNull { it != uid }
                    }
                    .distinct()

                if (otherUids.isEmpty()) {
                    dialogBinding.progressBar.visibility = View.GONE
                    dialogBinding.tvEmpty.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                val freelancers = mutableListOf<FreelancerItem>()
                var pending = otherUids.size

                otherUids.forEach { otherUid ->
                    db.collection("Users").document(otherUid).get()
                        .addOnSuccessListener { userDoc ->
                            val name = userDoc.getString("fullName") ?: "Unknown"
                            val email = userDoc.getString("email") ?: ""
                            val image = userDoc.getString("profilePictureUrl") ?: ""
                            freelancers.add(FreelancerItem(otherUid, name, email, image))
                            pending--
                            if (pending == 0) showFreelancerList(dialogBinding, freelancers, project, dialog)
                        }
                        .addOnFailureListener {
                            pending--
                            if (pending == 0) showFreelancerList(dialogBinding, freelancers, project, dialog)
                        }
                }
            }
            .addOnFailureListener { e ->
                if (_binding == null || !isAdded) return@addOnFailureListener
                Log.e("ASSIGN", "Chat fetch failed", e)
                dialogBinding.progressBar.visibility = View.GONE
                dialogBinding.tvEmpty.visibility = View.VISIBLE
            }
    }

    private fun showFreelancerList(
        dialogBinding: DialogAssignFromChatsBinding,
        freelancers: List<FreelancerItem>,
        project: Project,
        dialog: AlertDialog
    ) {
        if (_binding == null || !isAdded) {
            dialog.dismiss()
            return
        }

        dialogBinding.progressBar.visibility = View.GONE

        if (freelancers.isEmpty()) {
            dialogBinding.tvEmpty.visibility = View.VISIBLE
            return
        }

        val selectAdapter = FreelancerSelectAdapter(freelancers) { selected ->
            db.collection("Projects").document(project.projectid)
                .update(
                    "status", ProjectStatus.ASSIGNED.name,
                    "freeuid", selected.uid,
                    "freename", selected.name,
                    "freeemail", selected.email,
                    "startedAt", FieldValue.serverTimestamp()
                )
                .addOnSuccessListener {
                    if (isAdded) Toast.makeText(requireContext(), "Freelancer assigned!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
                .addOnFailureListener { e ->
                    Log.e("ASSIGN", "Update failed", e)
                    if (isAdded) Toast.makeText(requireContext(), "Failed to assign", Toast.LENGTH_SHORT).show()
                }
        }

        dialogBinding.rvFreelancers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = selectAdapter
            visibility = View.VISIBLE
        }
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
        val filtered = allProjects.filter { it.status == status }
        adapter.submitList(filtered)
        if (filtered.isEmpty()) {
            val (title, sub) = when (tabPosition) {
                0 -> Pair("No Open Projects", "Tap + to post a new project")
                1 -> Pair("No Projects In Progress", "Assign a freelancer to get started")
                2 -> Pair("No Completed Projects", "Completed projects will appear here")
                3 -> Pair("No Cancelled Projects", "Hope it stays that way!")
                else -> Pair("No Projects Yet", "Tap + to post a new project")
            }
            binding.tvEmpty.text = title
            binding.tvEmptySub.text = sub
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmptySub.visibility = View.VISIBLE
            binding.projectrecycler.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.tvEmptySub.visibility = View.GONE
            binding.projectrecycler.visibility = View.VISIBLE
        }
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