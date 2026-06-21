package com.nikhil.buyerapp.profile

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import com.nikhil.buyerapp.comprofile.supabasefile
import com.nikhil.buyerapp.databinding.FragmentProfileBinding
import com.nikhil.buyerapp.dataclasses.Client
import com.nikhil.buyerapp.dataclasses.User
import com.nikhil.buyerapp.utils.logd
import com.nikhil.buyerapp.utils.loge
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.UploadStatus
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.uploadAsFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class CoreProfileFragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null

    private val paymentMethods = listOf(
        "UPI", "PayPal", "Bank Transfer", "Credit Card",
        "Debit Card", "Crypto", "Net Banking", "Cash"
    )

    private val selectedPayments = mutableListOf<String>()

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val uid = auth.currentUser?.uid
    private lateinit var supabaseClient: SupabaseClient
    private val db = Firebase.firestore

    private lateinit var reviewAdapter: ReviewAdapter

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) uploadImageToSupabase(uri)
    }

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
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        supabaseClient = (requireActivity().application as supabasefile).supabaseClient

        setupReviewRecycler()
        loadinfo()
        loadotherinfo()

        binding.btnEditPhoto.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_prof_to_edit)
        }

        binding.btnManagePayment.setOnClickListener {
            showPaymentMethodsDialog()
        }

        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }

    // ─── Toast helper ───────────────────────────────────────────────────────

    private fun safeToast(message: String, long: Boolean = false) {
        if (!isAdded) return
        Toast.makeText(
            requireContext(),
            message,
            if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        ).show()
    }

    // ─── Settings ────────────────────────────────────────────────────────────

    private fun showSettingsDialog() {
        if (!isAdded) return

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_settings, null)

        val dialog = android.app.Dialog(requireContext()).apply {
            setContentView(dialogView)
            window?.apply {
                setBackgroundDrawable(
                    android.graphics.drawable.GradientDrawable().apply {
                        setColor(android.graphics.Color.WHITE)
                        cornerRadius = 32f * resources.displayMetrics.density
                    }
                )
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.88).toInt(),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }

        dialogView.findViewById<LinearLayout>(R.id.rowLogout).setOnClickListener {
            dialog.dismiss()
            logout()
        }

        dialogView.findViewById<LinearLayout>(R.id.rowDeleteAccount).setOnClickListener {
            dialog.dismiss()
            showDeleteConfirmationDialog()
        }

        dialogView.findViewById<LinearLayout>(R.id.rowContactSupport).setOnClickListener {
            dialog.dismiss()
            showcontactDialog()
        }

        dialog.show()
    }

    private fun showcontactDialog() {
        if (!isAdded) return

        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:support.skillvedika@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "Skill Vedika Support")
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            safeToast("No email app found. Contact us at support.skillvedika@gmail.com", long = true)
        }
    }

    private fun logout() {
        auth.signOut()
        navigateToLogin()
    }

    private fun showDeleteConfirmationDialog() {
        if (!isAdded) return

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_delete_confirm, null)

        val dialog = android.app.Dialog(requireContext()).apply {
            setContentView(dialogView)
            window?.apply {
                setBackgroundDrawable(
                    android.graphics.drawable.GradientDrawable().apply {
                        setColor(android.graphics.Color.WHITE)
                        cornerRadius = 32f * resources.displayMetrics.density
                    }
                )
                setLayout(
                    (resources.displayMetrics.widthPixels * 0.88).toInt(),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        }

        val etPassword = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPasswordinside)

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
            .setOnClickListener { dialog.dismiss() }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDeleteConfirminside)
            .setOnClickListener {
                val password = etPassword.text.toString().trim()
                if (password.isEmpty()) {
                    safeToast("Enter your password")
                    return@setOnClickListener
                }
                dialog.dismiss()
                deleteAccountCascade(password)
            }

        dialog.show()
    }

    private fun deleteAccountCascade(password: String) {
        val currentUid = uid ?: run {
            safeToast("Not signed in")
            return
        }

        val currentUser = auth.currentUser ?: run {
            safeToast("Not signed in")
            return
        }

        val email = currentUser.email ?: run {
            safeToast("Email not found")
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 0. Re-authenticate first — required before delete
                val credential = com.google.firebase.auth.EmailAuthProvider
                    .getCredential(email, password)
                currentUser.reauthenticate(credential).await()

                // 1. Delete Projects where clientuid == uid
                val projects = db.collection("Projects")
                    .whereEqualTo("clientuid", currentUid)
                    .get()
                    .await()

                projects.documents.chunked(500).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }

                // 2. Delete Chats where uid is a participant
                val chats = db.collection("Chat")
                    .whereArrayContains("participants", currentUid)
                    .get()
                    .await()

                chats.documents.chunked(500).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { batch.delete(it.reference) }
                    batch.commit().await()
                }

                // 3. Delete Client document
                db.collection("Client").document(currentUid).delete().await()

                // 4. Grab profile picture URL before deleting Users doc
                val userSnapshot = db.collection("Users").document(currentUid).get().await()
                val profilePicUrl = userSnapshot.getString("profilePictureUrl")

                // 5. Delete Users document
                db.collection("Users").document(currentUid).delete().await()

                // 6. Delete Supabase profile image if present
                profilePicUrl?.let { url ->
                    try {
                        val filePath = url.substringAfter("/object/public/sample/")
                        if (filePath.isNotEmpty()) {
                            supabaseClient.storage.from("sample").delete(listOf(filePath))
                        }
                    } catch (e: Exception) {
                        Log.e("DeleteAccount", "Supabase delete failed (non-fatal): ${e.message}")
                    }
                }

                // 7. Delete Firebase Auth user
                currentUser.delete().await()

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    safeToast("Account deleted successfully")
                    navigateToLogin()
                }

            } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    safeToast("Wrong password. Account not deleted.", long = true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("DeleteAccount", "Cascade delete failed: ${e.message}")
                    if (!isAdded) return@withContext
                    safeToast("Failed: ${e.message}", long = true)
                }
            }
        }
    }

    private fun navigateToLogin() {
        if (!isAdded) return
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        requireActivity().finish()
    }

    // ─── Payment Methods ──────────────────────────────────────────────────────

    private fun showPaymentMethodsDialog() {
        if (!isAdded) return

        val checkedItems = BooleanArray(paymentMethods.size) { i ->
            selectedPayments.contains(paymentMethods[i])
        }

        MaterialAlertDialogBuilder(requireContext(), R.style.CustomMaterialDialog)
            .setTitle("Select Payment Methods")
            .setMultiChoiceItems(paymentMethods.toTypedArray(), checkedItems) { _, which, isChecked ->
                val method = paymentMethods[which]
                if (isChecked) selectedPayments.add(method)
                else selectedPayments.remove(method)
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
            .update("paymentMethods", selectedPayments)
            .addOnFailureListener { e ->
                Log.e("SavePayment", "Failed to update payment methods: ${e.message}")
                safeToast("Failed to save payment methods")
            }
    }

    private fun updatePaymentChips() {
        if (_binding == null) return

        binding.chipPaymentMethods.removeAllViews()

        if (selectedPayments.isEmpty()) {
            binding.emptyPaymentState.visibility = View.VISIBLE
            return
        }

        binding.emptyPaymentState.visibility = View.GONE

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

    // ─── Data Loading ─────────────────────────────────────────────────────────

    private fun loadinfo() {
        if (uid == null) return

        db.collection("Users").document(uid)
            .addSnapshotListener { snapshot, error ->
                val b = _binding ?: return@addSnapshotListener
                if (!isAdded) return@addSnapshotListener
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val user = snapshot.toObject<User>()
                Glide.with(this@CoreProfileFragment)
                    .load(user?.profilePictureUrl)
                    .error(R.drawable.ic_launcher_background)
                    .into(b.profileImage)

                b.tvName.text = user?.fullName
            }
    }

    private fun loadotherinfo() {
        if (uid == null) return

        db.collection("Client").document(uid)
            .addSnapshotListener { snapshot, error ->
                val b = _binding ?: return@addSnapshotListener
                if (!isAdded) return@addSnapshotListener
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val user = snapshot.toObject<Client>()

                b.tvCompanyName.text = user?.companyName

                selectedPayments.clear()
                selectedPayments.addAll(user?.paymentMethods ?: emptyList())
                updatePaymentChips()

                val rating = user?.rating ?: 0.0
                if (rating > 0) {
                    b.tvRating.text = String.format("%.1f", rating)
                    b.tvRating.visibility = View.VISIBLE
                    b.tvNoRating.visibility = View.GONE
                } else {
                    b.tvRating.visibility = View.GONE
                    b.tvNoRating.visibility = View.VISIBLE
                }

                val reviews = user?.reviews ?: emptyList()
                if (reviews.isNotEmpty()) {
                    b.tvReviewCount.text = reviews.size.toString()
                    b.tvReviewCount.visibility = View.VISIBLE
                    b.tvNoReviewscount.visibility = View.GONE
                    b.rvRecentReviews.visibility = View.VISIBLE
                    b.emptyReviewsState.visibility = View.GONE
                    reviewAdapter.submitList(reviews.sortedByDescending { it.timestamp.seconds })
                } else {
                    b.tvReviewCount.visibility = View.GONE
                    b.tvNoReviewscount.visibility = View.VISIBLE
                    b.rvRecentReviews.visibility = View.GONE
                    b.emptyReviewsState.visibility = View.VISIBLE
                }

                loge("${user?.name}")
                logd("${user?.companyName}")
            }
    }

    // ─── Image Upload ─────────────────────────────────────────────────────────

    private fun setupReviewRecycler() {
        reviewAdapter = ReviewAdapter()
        binding.rvRecentReviews.apply {
            adapter = reviewAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            isNestedScrollingEnabled = false
        }
    }

    private fun uploadImageToSupabase(uri: Uri) {
        if (_binding == null || !isAdded) return

        val byteArray = uriToByteArray(requireContext(), uri)

        if (byteArray.size > 5 * 1024 * 1024) {
            safeToast("Image must be under 5 MB")
            return
        }

        binding.btnEditPhoto.isEnabled = false
        val fileName = "uploads/${System.currentTimeMillis()}.jpg"
        val bucket = supabaseClient.storage.from("sample")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                bucket.uploadAsFlow(fileName, byteArray).collect { status ->
                    withContext(Dispatchers.Main) {
                        if (_binding == null || !isAdded) return@withContext
                        when (status) {
                            is UploadStatus.Progress -> Log.d("Upload", "In progress...")
                            is UploadStatus.Success -> {
                                binding.btnEditPhoto.isEnabled = true
                                handleUploadSuccess(fileName)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("Upload", "Error: ${e.message}")
                    if (_binding == null || !isAdded) return@withContext
                    binding.btnEditPhoto.isEnabled = true
                    safeToast("Upload failed: ${e.message}")
                }
            }
        }
    }

    private fun handleUploadSuccess(fileName: String) {
        if (_binding == null || !isAdded) return

        try {
            val imageUrl = supabaseClient.storage.from("sample").publicUrl(fileName)
            val currentUser = auth.currentUser ?: run {
                safeToast("User authentication error")
                return
            }

            db.collection("Users").document(currentUser.uid)
                .update("profilePictureUrl", imageUrl)
                .addOnSuccessListener {
                    if (_binding == null || !isAdded) return@addOnSuccessListener
                    safeToast("Profile image updated!")
                    Glide.with(this)
                        .load(imageUrl)
                        .error(R.drawable.ic_launcher_background)
                        .into(binding.profileImage)
                }
                .addOnFailureListener { e ->
                    if (!isAdded) return@addOnFailureListener
                    safeToast("Failed to update image: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "handleUploadSuccess error: ${e.message}")
        }
    }

    private fun uriToByteArray(context: Context, uri: Uri): ByteArray {
        return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw Exception("Unable to read image")
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CoreProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}