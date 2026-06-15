package com.nikhil.buyerapp.Orders

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.firestore
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.FragmentAddProjectBinding
import com.nikhil.buyerapp.dataclasses.ProjectStatus
import com.nikhil.buyerapp.utils.UserUtils
import com.nikhil.buyerapp.utils.snack
import com.nikhil.sellerapp.skills.SkillData
import kotlinx.coroutines.launch
import java.sql.Timestamp

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AddProjectFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AddProjectFragment : Fragment() {
    private var _binding:FragmentAddProjectBinding?=null
    private val binding get()=_binding!!
    private val selectedSkillsList = mutableListOf<String>()
    private var currentAvailableSkills: List<String> = emptyList()
    private var clientName: String = "Loading..."
    private val db=Firebase.firestore
    private val auth:FirebaseAuth=FirebaseAuth.getInstance()
    val uid=auth.currentUser?.uid
    private var param1: String? = null
    private var param2: String? = null

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
        _binding=FragmentAddProjectBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            // This one line does all the heavy lifting
            clientName = UserUtils.fetchCurrentUserName()
        }
        setupCategoryDropdown()
        setupSkillButton()
        binding.btnPostJob.setOnClickListener {
            savejob()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AddProjectFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AddProjectFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
//    @DocumentId
//    val projectid:String="",








    private fun savejob()
    {
        val projtitle=binding.etTitle.text.toString()
        val cat=binding.etCategory.text.toString()
        val clientuid=uid
        val clientname=clientName
        val desc=binding.etDesc.text.toString()
        val budgetStr = binding.etBudget.text.toString().trim()
        val budget = budgetStr.toDoubleOrNull() ?: 0.0

        if (selectedSkillsList.isEmpty()) {
            snack("Please select at least one skill")
            return
        }
        val newProjectId = db.collection("Projects").document().id
        val new= mapOf(
            "projectid" to newProjectId,
            "clientuid" to clientuid,
            "clientName" to clientname,
            "title" to projtitle,
            "description" to desc,
            "status" to ProjectStatus.OPEN.name,
            "budget" to budget,
            "requiredSkills" to selectedSkillsList,
            "category" to cat,

            )
        binding.btnPostJob.isEnabled = false
        db.collection("Projects").document(newProjectId).set(new).addOnSuccessListener {
            snack("Job posted")
            findNavController().navigateUp()
        }.addOnFailureListener { e->
            binding.btnPostJob.isEnabled=true
            snack("Error:${e.message}")

        }


    }
    private fun setupCategoryDropdown() {
        // A. Get List of Categories from your Object
        val categories = SkillData.getSkillCategories().map { it.categoryName }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.etCategory.setAdapter(adapter)

        // B. Listen for Selection
        binding.etCategory.setOnItemClickListener { _, _, position, _ ->
            val selectedCategoryName = adapter.getItem(position)

            // C. Find the matching skills from your Object
            val categoryObj = SkillData.getSkillCategories().find { it.categoryName == selectedCategoryName }

            if (categoryObj != null) {
                // Update the available list
                currentAvailableSkills = categoryObj.skills

                // Clear previous selections if category changes
                selectedSkillsList.clear()
                binding.chipGroupSkills.removeAllViews()

                // Enable the Add button
                binding.btnAddSkill.isEnabled = true
            }
        }
    }

    private fun setupSkillButton() {
        binding.btnAddSkill.isEnabled = false // Disable until category picked

        binding.btnAddSkill.setOnClickListener {
            if (currentAvailableSkills.isEmpty()) return@setOnClickListener

            // Convert list to Array for the Dialog
            val skillsArray = currentAvailableSkills.toTypedArray()
            val checkedItems = BooleanArray(skillsArray.size) { false }

            // Show Multi-Select Dialog
            MaterialAlertDialogBuilder(requireContext(),R.style.CustomMaterialDialog)
                .setTitle("Select Required Skills")
                .setMultiChoiceItems(skillsArray, checkedItems) { dialog, which, isChecked ->
                    // You can handle realtime checks here if needed
                }
                .setPositiveButton("Add") { dialog, _ ->
                    // 1. Loop through the ListView of the dialog to find checked items
                    val listView = (dialog as androidx.appcompat.app.AlertDialog).listView

                    for (i in 0 until listView.count) {
                        if (listView.isItemChecked(i)) {
                            val skill = skillsArray[i]
                            addSkillChip(skill)
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // HELPER: Add a visual Chip
    private fun addSkillChip(skillName: String) {
        if (selectedSkillsList.contains(skillName)) return // Don't add duplicates

        selectedSkillsList.add(skillName)

        val chip = Chip(requireContext())
        chip.text = skillName
        chip.isCloseIconVisible = true // Allow user to remove it
        chip.setOnCloseIconClickListener {
            binding.chipGroupSkills.removeView(chip)
            selectedSkillsList.remove(skillName)
        }

        binding.chipGroupSkills.addView(chip)
    }
}
