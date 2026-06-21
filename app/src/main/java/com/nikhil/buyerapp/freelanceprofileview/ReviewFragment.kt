package com.nikhil.buyerapp.freelanceprofileview

import com.nikhil.buyerapp.databinding.FragmentReviewBinding

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.nikhil.buyerapp.dataclasses.Review
import com.nikhil.buyerapp.utils.loge
import java.util.Date

class ReviewFragment : Fragment() {

    private var _binding: FragmentReviewBinding? = null
    private val binding get() = _binding!!

    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    private var clientUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clientUid = arguments?.getString("uid")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCharCounter()

        binding.imgbt.setOnClickListener {
            if (isAdded) findNavController().popBackStack()
        }

        binding.btnsave.setOnClickListener {
            loge(clientUid ?: "null")
            submitReview()
        }
    }

    private fun setupCharCounter() {
        binding.etReview.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (_binding == null) return
                    binding.tvCharCount.text = "${s?.length ?: 0}/300"
                }

                override fun afterTextChanged(s: Editable?) {}
            }
        )
    }

    private fun showToast(message: String, long: Boolean = false) {
        if (!isAdded || _binding == null) return
        Toast.makeText(
            requireContext(),
            message,
            if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        ).show()
    }

    private fun submitReview() {

        if (!isAdded || _binding == null) return

        val reviewerUid = auth.currentUser?.uid ?: return
        val targetClientUid = clientUid ?: return

        val rating = binding.ratingBar.rating.toInt()
        val reviewText = binding.etReview.text.toString().trim()

        // VALIDATION
        if (rating == 0) {
            showToast("Please select rating")
            return
        }

        if (reviewText.isEmpty()) {
            showToast("Please write review")
            return
        }

        // GET REVIEWER NAME
        db.collection("Users")
            .document(reviewerUid)
            .get()
            .addOnSuccessListener { userDoc ->

                if (!isAdded || _binding == null) {
                    return@addOnSuccessListener
                }

                val reviewerName = userDoc.getString("fullName") ?: "Anonymous"

                val review = Review(
                    reviewerUid = reviewerUid,
                    reviewerName = reviewerName,
                    rating = rating,
                    reviewText = reviewText,
                    timestamp = Timestamp(Date())
                )

                saveReview(targetClientUid, review, rating.toDouble())
            }
            .addOnFailureListener { e ->
                loge("REVIEW_ERROR", e)
                showToast(e.localizedMessage ?: "Failed to fetch user info", long = true)
            }
    }

    private fun saveReview(
        clientUid: String,
        review: Review,
        newRating: Double
    ) {

        val clientRef = db.collection("Freelancers").document(clientUid)

        db.runTransaction { transaction ->

            val snapshot = transaction.get(clientRef)

            val oldRating = snapshot.getDouble("rating") ?: 0.0

            val oldReviews = snapshot.get("reviews") as? List<*> ?: emptyList<Any>()

            val reviewCount = oldReviews.size

            val updatedRating =
                ((oldRating * reviewCount) + newRating) / (reviewCount + 1)

            transaction.update(
                clientRef,
                mapOf(
                    "reviews" to FieldValue.arrayUnion(review),
                    "rating" to updatedRating
                )
            )
        }
            .addOnSuccessListener {

                if (!isAdded || _binding == null) return@addOnSuccessListener

                showToast("Review submitted")
                findNavController().popBackStack()
            }
            .addOnFailureListener { e ->

                loge("REVIEW_ERROR", e)

                if (!isAdded || _binding == null) return@addOnFailureListener

                showToast(e.localizedMessage ?: "Failed to submit review", long = true)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}