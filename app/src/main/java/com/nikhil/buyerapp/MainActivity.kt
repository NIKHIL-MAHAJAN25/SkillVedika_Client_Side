package com.nikhil.buyerapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.nikhil.buyerapp.Signup.SignupActivity
import com.nikhil.buyerapp.basichome.hosthome
import com.nikhil.buyerapp.comprofile.EnterCode
import com.nikhil.buyerapp.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val uid = auth.currentUser?.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.tvtitlee.visibility = View.INVISIBLE
        binding.tvtitlee2.visibility = View.INVISIBLE

        binding.lottiee.setAnimation(R.raw.animaa)
        binding.lottiee.repeatCount = 0
        binding.lottiee.speed = 2.0f

        binding.lottiee.addLottieOnCompositionLoadedListener { composition ->
            binding.lottiee.playAnimation()

            binding.tvtitlee.visibility = View.VISIBLE
            binding.tvtitlee2.visibility = View.VISIBLE

            binding.tvtitlee.alpha = 0f
            binding.tvtitlee2.alpha = 0f

            binding.tvtitlee.animate().alpha(1f).setDuration(300).start()
            binding.tvtitlee2.animate().alpha(1f).setDuration(500).start()

            lifecycleScope.launch {

                delay(3500L)

                val user = auth.currentUser

                if (user == null) {
                    navigateTo(SignupActivity::class.java)
                    return@launch
                }

                val status = checkAccountStatus(user.uid)

                val destination = when {
                    !status.profileComplete -> SignupActivity::class.java
                    !status.approved -> EnterCode::class.java   // profile done, OTP not yet verified
                    else -> hosthome::class.java
                }

                navigateTo(destination)
            }
        }
    }

    private fun navigateTo(destClass: Class<*>) {
        val intent = Intent(this, destClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private data class AccountStatus(
        val profileComplete: Boolean,
        val approved: Boolean
    )

    // one-time check on app launch; use snapshot listeners for realtime screens
    private suspend fun checkAccountStatus(uid: String): AccountStatus {
        return try {
            val document = db.collection("Users").document(uid).get().await()
            AccountStatus(
                profileComplete = document.getBoolean("profilecomplete") ?: false,
                approved = document.getBoolean("approved") ?: false
            )
        } catch (e: Exception) {
            AccountStatus(profileComplete = false, approved = false)
        }
    }
}