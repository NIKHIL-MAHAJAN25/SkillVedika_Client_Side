package com.nikhil.buyerapp.Orders

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.nikhil.buyerapp.databinding.FragmentAddProjectBinding
import com.nikhil.buyerapp.dataclasses.ProjectStatus
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.utils.UserUtils
import com.nikhil.buyerapp.utils.snack
import com.nikhil.buyerapp.skills.SkillData
import kotlinx.coroutines.launch

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class AddProjectFragment : Fragment() {

    private var _binding: FragmentAddProjectBinding? = null
    private val binding get() = _binding!!

    private val selectedSkillsList = mutableListOf<String>()
    private var currentAvailableSkills: List<String> = emptyList()
    private var clientName: String = "Loading..."

    private val db = Firebase.firestore
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid

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
    ): View {
        _binding = FragmentAddProjectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Bottom input moves with keyboard
        ViewCompat.setOnApplyWindowInsetsListener(binding.etDesc) { v, windowInsets ->
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, maxOf(imeInsets.bottom, systemBars.bottom))
            windowInsets
        }

        lifecycleScope.launch {
            val name = UserUtils.fetchCurrentUserName()
            if (_binding == null || !isAdded) return@launch
            clientName = name
        }

        setupCategoryDropdown()
        setupSkillButton()

        binding.btnPostJob.setOnClickListener {
            savejob()
        }

        // Auto-clear errors on input
        listOf(
            binding.layoutTitle to binding.etTitle,
            binding.layoutBudget to binding.etBudget,
            binding.layoutDesc to binding.etDesc
        ).forEach { (layout, editText) ->
            editText.addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (_binding == null) return
                    layout.error = null
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        // NOTE: category selection logic lives in setupCategoryDropdown() only.
        // (Previously duplicated here — the duplicate listener silently overwrote
        // setupCategoryDropdown()'s, so it was removed rather than kept as dead code.)
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            AddProjectFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun savejob() {

        if (_binding == null) return

        val projtitle = binding.etTitle.text.toString()
        val cat = binding.etCategory.text.toString()
        val clientuid = uid
        val clientname = clientName
        val desc = binding.etDesc.text.toString()
        val budgetStr = binding.etBudget.text.toString().trim()
        val budget = budgetStr.toDoubleOrNull() ?: 0.0

        binding.layoutTitle.error = null
        binding.layoutCategory.error = null
        binding.layoutBudget.error = null
        binding.layoutDesc.error = null

        when {
            projtitle.isEmpty() -> {
                binding.layoutTitle.error = "Project title is required"
                binding.etTitle.requestFocus()
                return
            }
            projtitle.length < 5 -> {
                binding.layoutTitle.error = "Title is too short"
                binding.etTitle.requestFocus()
                return
            }
            cat.isEmpty() -> {
                binding.layoutCategory.error = "Please select a category"
                binding.etCategory.requestFocus()
                return
            }
            selectedSkillsList.isEmpty() -> {
                snack("Please add at least one required skill")
                binding.btnAddSkill.requestFocus()
                return
            }
            budgetStr.isEmpty() -> {
                binding.layoutBudget.error = "Budget is required"
                binding.etBudget.requestFocus()
                return
            }
            budgetStr.toDoubleOrNull() == null -> {
                binding.layoutBudget.error = "Enter a valid number"
                binding.etBudget.requestFocus()
                return
            }
            budgetStr.toDouble() <= 0 -> {
                binding.layoutBudget.error = "Budget must be greater than 0"
                binding.etBudget.requestFocus()
                return
            }
            desc.isEmpty() -> {
                binding.layoutDesc.error = "Description is required"
                binding.etDesc.requestFocus()
                return
            }
            desc.length < 20 -> {
                binding.layoutDesc.error = "Please describe requirements in more detail"
                binding.etDesc.requestFocus()
                return
            }
        }

        // (Removed redundant unreachable `if (selectedSkillsList.isEmpty())` check —
        // already handled inside the `when` above.)

        val newProjectId = db.collection("Projects").document().id
        val new = mapOf(
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

        db.collection("Projects").document(newProjectId).set(new)
            .addOnSuccessListener {
                if (_binding == null || !isAdded) return@addOnSuccessListener
                snack("Job posted")
                findNavController().navigateUp()
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener
                binding.btnPostJob.isEnabled = true
                snack("Error:${e.message}")
            }
    }

    private fun setupCategoryDropdown() {
        val categories = SkillData.getSkillCategories().map { it.categoryName }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.etCategory.setAdapter(adapter)

        binding.etCategory.setOnItemClickListener { _, _, position, _ ->
            if (_binding == null) return@setOnItemClickListener

            binding.layoutCategory.error = null

            val selectedCategoryName = adapter.getItem(position)
            val categoryObj = SkillData.getSkillCategories().find { it.categoryName == selectedCategoryName }

            if (categoryObj != null) {
                currentAvailableSkills = categoryObj.skills
                selectedSkillsList.clear()
                binding.chipGroupSkills.removeAllViews()
                binding.btnAddSkill.isEnabled = true
            }
        }
    }

    private fun setupSkillButton() {
        binding.btnAddSkill.isEnabled = false

        binding.btnAddSkill.setOnClickListener {
            if (currentAvailableSkills.isEmpty()) return@setOnClickListener

            val skillsArray = currentAvailableSkills.toTypedArray()
            val checkedItems = BooleanArray(skillsArray.size) { false }

            MaterialAlertDialogBuilder(requireContext(), R.style.CustomMaterialDialog)
                .setTitle("Select Required Skills")
                .setMultiChoiceItems(skillsArray, checkedItems) { _, _, _ -> }
                .setPositiveButton("Add") { dialog, _ ->
                    val listView = (dialog as androidx.appcompat.app.AlertDialog).listView
                    for (i in 0 until listView.count) {
                        if (listView.isItemChecked(i)) {
                            addSkillChip(skillsArray[i])
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun addSkillChip(skillName: String) {
        if (_binding == null) return
        if (selectedSkillsList.contains(skillName)) return

        selectedSkillsList.add(skillName)

        val chip = Chip(requireContext())
        chip.text = skillName
        chip.isCloseIconVisible = true
        chip.setOnCloseIconClickListener {
            if (_binding == null) return@setOnCloseIconClickListener
            binding.chipGroupSkills.removeView(chip)
            selectedSkillsList.remove(skillName)
        }

        binding.chipGroupSkills.addView(chip)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}