package com.nikhil.buyerapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.nikhil.buyerapp.databinding.ActivityMainBinding
import com.nikhil.buyerapp.utils.Navigateto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val auth:FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val uid=auth.currentUser?.uid

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

            binding.tvtitlee.animate()
                .alpha(1f)
                .setDuration(300)
                .start()

            binding.tvtitlee2.animate()
                .alpha(1f)
                .setDuration(500)
                .start()

            lifecycleScope.launch {

                delay(3500L)

                val user = auth.currentUser

                // User not logged in
                if (user == null) {

                    navigateTo(SignupActivity::class.java)

                    return@launch
                }

                // Check profile completion
                val profComplete = check(user.uid)

                val destination =
                    if (profComplete) {
                        hosthome::class.java
                    } else {
                        SignupActivity::class.java
                    }

                navigateTo(destination)
            }



//            val duration = (composition?.duration ?: 3000L).toFloat()/(binding.lottiee.speed)
//
//            Handler(Looper.getMainLooper()).postDelayed({
//
//                startActivity(Intent(this, SignupActivity::class.java))
//                finish()
//            }, duration.toLong())
        }
    }


//    override fun onStart() {
//        super.onStart()
//
//        // Get current user (this will be persisted by Firebase if everything is set up correctly)
//        val user = auth.currentUser
//
//        // If user is null -> not signed in -> go to SignUpActivity
//        if (user == null) {
//            navigateTo(SignupActivity::class.java)
//            return
//        }
//
//        // If user exists, check profile completion in Firestore and route accordingly
//        lifecycleScope.launch {
//            val uid = user.uid
//            val profComplete = check(uid) // returns true/false (never null)
//            val destClass = if (profComplete) hosthome::class.java else SignupActivity::class.java
//
//            // Optional splash delay for animation; adjust or remove as desired
//            delay(3500L)
//            navigateTo(destClass)
//        }
//    }
    private fun navigateTo(destClass: Class<*>) {
        val intent = Intent(this, destClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    // we are using suspend function because its a one time check for realtime calls always use snapshot listener as in notepad file
    private suspend fun check(uid: String): Boolean {
        return try {
            val document = db.collection("Users").document(uid).get().await()
            // safely return boolean; if field is missing default to false
            document.getBoolean("profilecomplete") ?: false
        } catch (e: Exception) {

            false
        }
    }
}