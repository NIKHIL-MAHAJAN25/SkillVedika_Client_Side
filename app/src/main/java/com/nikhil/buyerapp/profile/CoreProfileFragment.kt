package com.nikhil.buyerapp.profile

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.nikhil.buyerapp.Login.LoginActivity
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.FragmentProfileBinding
import com.nikhil.buyerapp.dataclasses.Client
import com.nikhil.buyerapp.dataclasses.User
import com.nikhil.buyerapp.utils.logd
import com.nikhil.buyerapp.utils.loge

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class CoreProfileFragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null

    private val paymentMethods = listOf(
        "UPI",
        "PayPal",
        "Bank Transfer",
        "Credit Card",
        "Debit Card",
        "Crypto",
        "Net Banking",
        "Cash"
    )

    private val selectedPayments = mutableListOf<String>()

    private var _binding: FragmentProfileBinding? = null

    private val binding get() = _binding!!

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val uid = auth.currentUser?.uid

    private val db = Firebase.firestore

    private lateinit var reviewAdapter: ReviewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            FragmentProfileBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        setupReviewRecycler()

        loadinfo()

        loadotherinfo()

        binding.btnEditProfile.setOnClickListener {

            findNavController().navigate(
                R.id.action_prof_to_edit
            )
        }

        binding.btnManagePayment.setOnClickListener {

            showPaymentMethodsDialog()
        }

        binding.btnLogout.setOnClickListener {

            FirebaseAuth.getInstance().signOut()

            val intent =
                Intent(
                    requireContext(),
                    LoginActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            requireActivity().finish()
        }
    }

    private fun setupReviewRecycler() {

        reviewAdapter = ReviewAdapter()

        binding.rvRecentReviews.apply {

            adapter = reviewAdapter

            layoutManager =
                LinearLayoutManager(
                    requireContext(),
                    LinearLayoutManager.HORIZONTAL,
                    false
                )

            isNestedScrollingEnabled = false
        }
    }

    private fun showPaymentMethodsDialog() {

        val checkedItems =
            BooleanArray(paymentMethods.size)

        paymentMethods.forEachIndexed { index, method ->

            checkedItems[index] =
                selectedPayments.contains(method)
        }

        MaterialAlertDialogBuilder(
            requireContext(),
            R.style.CustomMaterialDialog
        )

            .setTitle("Select Payment Methods")

            .setMultiChoiceItems(
                paymentMethods.toTypedArray(),
                checkedItems
            ) { _, which, isChecked ->

                val selectedMethod =
                    paymentMethods[which]

                if (isChecked) {

                    selectedPayments.add(
                        selectedMethod
                    )

                } else {

                    selectedPayments.remove(
                        selectedMethod
                    )
                }
            }

            .setPositiveButton("Save") { _, _ ->

                updatePaymentChips()

                savePaymentMethodsToFirestore()
            }

            .setNegativeButton("Cancel", null)

            .show()
    }

    private fun savePaymentMethodsToFirestore() {

        val currentUid = uid ?: return

        db.collection("Client")
            .document(currentUid)
            .update(
                "paymentMethods",
                selectedPayments
            )
    }

    private fun updatePaymentChips() {

        binding.chipPaymentMethods.removeAllViews()

        if (selectedPayments.isEmpty()) {

            binding.emptyPaymentState.visibility =
                View.VISIBLE

            return
        }

        binding.emptyPaymentState.visibility =
            View.GONE

        selectedPayments.forEach { method ->

            val chip = Chip(requireContext(), null, R.style.MyCustomChip).apply {
                text = method
                isCloseIconVisible = false
                isCheckable = false
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.bg)
                )
                setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            }

            binding.chipPaymentMethods.addView(chip)
        }
    }

    private fun loadotherinfo() {

        if (uid != null) {

            db.collection("Client")
                .document(uid)

                .addSnapshotListener { snapshot, error ->

                    val b =
                        _binding ?: return@addSnapshotListener

                    if (error != null) {

                        return@addSnapshotListener
                    }

                    if (snapshot != null &&
                        snapshot.exists()
                    ) {

                        val user =
                            snapshot.toObject<Client>()

                        // COMPANY
                        b.tvCompanyName.text =
                            user?.companyName

                        // PAYMENT METHODS
                        selectedPayments.clear()

                        selectedPayments.addAll(
                            user?.paymentMethods
                                ?: emptyList()
                        )

                        updatePaymentChips()

                        // RATING
                        val rating =
                            user?.rating ?: 0.0

                        if (rating > 0) {

                            b.tvRating.text =
                                String.format(
                                    "%.1f",
                                    rating
                                )

                            b.tvRating.visibility =
                                View.VISIBLE

                            b.tvNoRating.visibility =
                                View.GONE

                        } else {

                            b.tvRating.visibility =
                                View.GONE

                            b.tvNoRating.visibility =
                                View.VISIBLE
                        }

                        // REVIEWS
                        val reviews =
                            user?.reviews
                                ?: emptyList()

                        if (reviews.isNotEmpty()) {

                            b.tvReviewCount.text =
                                reviews.size.toString()

                            b.tvReviewCount.visibility =
                                View.VISIBLE

                            b.tvNoReviewscount.visibility =
                                View.GONE

                            b.rvRecentReviews.visibility =
                                View.VISIBLE

                            b.emptyReviewsState.visibility =
                                View.GONE

                            val recentReviews =
                                reviews.sortedByDescending {
                                    it.timestamp.seconds
                                }

                            reviewAdapter.submitList(
                                recentReviews
                            )

                        } else {

                            b.tvReviewCount.visibility =
                                View.GONE

                            b.tvNoReviewscount.visibility =
                                View.VISIBLE

                            b.rvRecentReviews.visibility =
                                View.GONE

                            b.emptyReviewsState.visibility =
                                View.VISIBLE
                        }

                        loge("${user?.name}")

                        logd("${user?.companyName}")
                    }
                }
        }
    }

    private fun loadinfo() {

        if (uid != null) {

            db.collection("Users")
                .document(uid)

                .addSnapshotListener { snapshot, error ->

                    val b =
                        _binding ?: return@addSnapshotListener

                    if (error != null) {

                        return@addSnapshotListener
                    }

                    if (snapshot != null &&
                        snapshot.exists()
                    ) {

                        val user =
                            snapshot.toObject<User>()

                        Glide.with(
                            this@CoreProfileFragment
                        )

                            .load(user?.profilePictureUrl)

                            .error(
                                R.drawable.ic_launcher_background
                            )

                            .into(binding.profileImage)

                        b.tvName.text =
                            user?.fullName
                    }
                }
        }
    }

    companion object {

        @JvmStatic
        fun newInstance(
            param1: String,
            param2: String
        ) =
            CoreProfileFragment().apply {

                arguments = Bundle().apply {

                    putString(ARG_PARAM1, param1)

                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}