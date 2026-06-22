package com.nikhil.buyerapp.Signup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.FieldValue

import com.nikhil.buyerapp.Login.LoginActivity
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.comprofile.ProfileScreen1
import com.nikhil.buyerapp.comprofile.ProfileScreen2
import com.nikhil.buyerapp.databinding.ActivitySignup2Binding
import com.nikhil.buyerapp.databinding.ActivitySignupBinding
import com.nikhil.buyerapp.dataclasses.User
import com.nikhil.buyerapp.dataclasses.UserRole


class SignupActivity2 : AppCompatActivity() {
    lateinit var binding: ActivitySignup2Binding
    private var auth:FirebaseAuth=FirebaseAuth.getInstance()
    val db= Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignup2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.alrsignin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        setupTermsText()
        binding.btnSignUP.setOnClickListener {
            val aemail = binding.etmailsignin2.text.toString()
            val password = binding.etpsswrdsignin2.text.toString()
            val confirm = binding.etpsswrdsignin3.text.toString()
            val date = FieldValue.serverTimestamp()
            if (aemail.isBlank() || password.isBlank() || confirm.isBlank()) {
                showError("Please fill all fields")
                return@setOnClickListener
            }
            if (password != confirm) {
                showError("Passwords do not match")
                return@setOnClickListener
            }
            if (!isPasswordStrong(password)) {
                showError("Password must be 8+ characters with uppercase, lowercase, number & symbol")
                return@setOnClickListener
            }
            auth.createUserWithEmailAndPassword(aemail, password).addOnSuccessListener {
                val intent = Intent(this, SignupActivity2::class.java)

                val auid = auth.currentUser?.uid
                val user = mapOf(
                    "email" to aemail,
                    "uid" to auid,
                    "profilecomplete" to false,
                    "createdon" to date,
                    "approved" to false,
                    "userole" to UserRole.CLIENT.name

                )
                if (auid != null) {
                    db.collection("Users").document(auid).set(user, SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, ProfileScreen1::class.java))

                        }
                }


            }.addOnFailureListener { e ->
                if (e is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                    db.collection("Users").whereEqualTo("email", aemail).get()
                        .addOnSuccessListener { docs ->
                            val existingRole = docs.documents.firstOrNull()?.getString("userole")
                            when (existingRole) {
                                "CLIENT" -> showError("This email is already registered as a Client. Please use the Client app to sign in.")
                                "FREELANCER" -> showError("This email is already registered as a Freelancer.")
                                else -> showError("An account already exists with this email.")
                            }
                        }
                        .addOnFailureListener {
                            showError("An account already exists with this email.")
                        }
                } else {
                    showError(e.message ?: "Signup failed")
                }
            }
        }
    }
    private fun showError(message: String) {
        com.google.android.material.snackbar.Snackbar
            .make(binding.root, message, com.google.android.material.snackbar.Snackbar.LENGTH_SHORT)
            .setBackgroundTint(android.graphics.Color.parseColor("#D9534F"))
            .setTextColor(android.graphics.Color.WHITE)
            .show()
    }
    private fun isPasswordStrong(password: String): Boolean {
        val hasMinLength = password.length >= 8
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }
        return hasMinLength && hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar
    }
    private fun setupTermsText() {
        val text = "By signing up, you agree to our Terms & Conditions and Privacy Policy"
        val spannable = SpannableString(text)
        val linkColor = android.graphics.Color.parseColor("#698B6A")

        val termsStart = text.indexOf("Terms & Conditions")
        val termsEnd = termsStart + "Terms & Conditions".length
        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://glowing-tiramisu-799c27.netlify.app/")))
            }
            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.color = linkColor
                ds.isUnderlineText = false // set true if you want underline
            }
        }, termsStart, termsEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        val privacyStart = text.indexOf("Privacy Policy")
        val privacyEnd = privacyStart + "Privacy Policy".length
        spannable.setSpan(object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://incomparable-panda-d9b537.netlify.app/")))
            }
            override fun updateDrawState(ds: android.text.TextPaint) {
                super.updateDrawState(ds)
                ds.color = linkColor
                ds.isUnderlineText = false // set true if you want underline
            }
        }, privacyStart, privacyEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.tvTerms.text = spannable
        binding.tvTerms.movementMethod = LinkMovementMethod.getInstance()
    }
}