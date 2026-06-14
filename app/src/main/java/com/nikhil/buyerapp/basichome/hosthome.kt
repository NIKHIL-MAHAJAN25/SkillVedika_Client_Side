package com.nikhil.buyerapp.basichome

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetView
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader


import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.ActivityHosthomeBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class hosthome : AppCompatActivity() {
    lateinit var binding: ActivityHosthomeBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getSharedPreferences("hints", Context.MODE_PRIVATE).edit().clear().apply()
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        binding=ActivityHosthomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(0, statusBars.top, 0, 0)
            insets
        }






        val navHostFragment=supportFragmentManager
            .findFragmentById(R.id.host) as NavHostFragment
        val navController:NavController=navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        // 3. Setup the BottomNavigationView with the NavController
        // (Also, use your binding variable instead of findViewById for consistency)


        // 4. Listen for navigation changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.home,
                R.id.chat,
                R.id.orders -> showBottomNav()
                else -> hideBottomNav()
            }
        }
        val prefs = getSharedPreferences("hints", MODE_PRIVATE)
        showHintsInSequence(prefs)
        }

    private fun showHintsInSequence(prefs: SharedPreferences) {
        binding.bottomNavigation.post {
            val menu = binding.bottomNavigation.getChildAt(0) as? ViewGroup ?: return@post
            val homeshown= prefs.getBoolean("home_hint_shows",false)
            val aiShown = prefs.getBoolean("ai_hint_shown", false)
            val profileShown = prefs.getBoolean("freelancer_full_profile_hint_shown", false)
            val ordershown = prefs.getBoolean("order_hint_shows",false)
            val chatshown = prefs.getBoolean("chat_hint_shows",false)




            when {
                !homeshown -> showHomeHint(prefs, menu)
                !chatshown -> showChatHint(prefs, menu)
                !ordershown -> showOrderHint(prefs, menu)
                !aiShown -> showAiHint(prefs, menu)
                !profileShown -> showProfileHint(prefs, menu)
            }
        }
    }
    private fun showHomeHint(prefs: SharedPreferences, menu: ViewGroup) {
        val aiTarget = menu.getChildAt(0) ?: return  // index 3 = AI/Gemini

        TapTargetView.showFor(
            this@hosthome,
            TapTarget.forView(
                aiTarget,
                "Start Exploring",
                "Find services, discover opportunities, and keep up with industry news all in one place."
            )
                .outerCircleColor(R.color.black)
                .outerCircleAlpha(0.85f)
                .targetCircleColor(android.R.color.white)
                .titleTextColor(android.R.color.white)
                .descriptionTextColor(android.R.color.white)
                .tintTarget(true)
                .drawShadow(true)
                .cancelable(false)
                .id(201),
            object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView) {
                    super.onTargetClick(view)
                    prefs.edit().putBoolean("home_hint_shows", true).apply()
                    // Show profile hint right after AI hint is dismissed
                   showChatHint(prefs,menu)
                }
            }
        )
    }
    private fun showChatHint(prefs: SharedPreferences, menu: ViewGroup) {
        val aiTarget = menu.getChildAt(1) ?: return  // index 3 = AI/Gemini

        TapTargetView.showFor(
            this@hosthome,
            TapTarget.forView(
                aiTarget,
                "Connect with People",
                "All your conversations appear here. Reply quickly, discuss project details, and build trust with clients."
            )
                .outerCircleColor(R.color.black)
                .outerCircleAlpha(0.85f)
                .targetCircleColor(android.R.color.white)
                .titleTextColor(android.R.color.white)
                .descriptionTextColor(android.R.color.white)
                .tintTarget(true)
                .drawShadow(true)
                .cancelable(false)
                .id(201),
            object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView) {
                    super.onTargetClick(view)
                    prefs.edit().putBoolean("chat_hint_shows", true).apply()
                    // Show profile hint right after AI hint is dismissed
                    showOrderHint(prefs, menu)
                }
            }
        )
    }
    private fun showOrderHint(prefs: SharedPreferences, menu: ViewGroup) {
        val aiTarget = menu.getChildAt(2) ?: return  // index 3 = AI/Gemini

        TapTargetView.showFor(
            this@hosthome,
            TapTarget.forView(
                aiTarget,
                "Manage Your Projects",
                "Track all your projects here. Use the tabs to view Open, In Progress, Completed, and Cancelled work."
            )
                .outerCircleColor(R.color.black)
                .outerCircleAlpha(0.85f)
                .targetCircleColor(android.R.color.white)
                .titleTextColor(android.R.color.white)
                .descriptionTextColor(android.R.color.white)
                .tintTarget(true)
                .drawShadow(true)
                .cancelable(false)
                .id(201),
            object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView) {
                    super.onTargetClick(view)
                    prefs.edit().putBoolean("order_hint_shows", true).apply()
                    // Show profile hint right after AI hint is dismissed
                    showAiHint(prefs, menu)
                }
            }
        )
    }

    private fun showAiHint(prefs: SharedPreferences, menu: ViewGroup) {
        val aiTarget = menu.getChildAt(3) ?: return  // index 3 = AI/Gemini

        TapTargetView.showFor(
            this@hosthome,
            TapTarget.forView(
                aiTarget,
                "AI assisted Hiring",
                "Smart Matching, Project Insights and much More"
            )
                .outerCircleColor(R.color.black)
                .outerCircleAlpha(0.85f)
                .targetCircleColor(android.R.color.white)
                .titleTextColor(android.R.color.white)
                .descriptionTextColor(android.R.color.white)
                .tintTarget(true)
                .drawShadow(true)
                .cancelable(false)
                .id(201),
            object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView) {
                    super.onTargetClick(view)
                    prefs.edit().putBoolean("ai_hint_shown", true).apply()
                    showProfileHint(prefs, menu)

                }
            }
        )
    }

    private fun showProfileHint(prefs: SharedPreferences, menu: ViewGroup) {
        val profileTarget = menu.getChildAt(4) ?: return  // index 4 = Profile

        TapTargetView.showFor(
            this@hosthome,
            TapTarget.forView(
                profileTarget,
                "Complete your Profile",
                "Add your Company Name and Payment Methods"
            )
                .outerCircleColor(R.color.black)
                .outerCircleAlpha(0.85f)
                .targetCircleColor(android.R.color.white)
                .titleTextColor(android.R.color.white)
                .descriptionTextColor(android.R.color.white)
                .tintTarget(true)
                .drawShadow(true)
                .cancelable(false)
                .id(202),
            object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView) {
                    super.onTargetClick(view)
                    prefs.edit()
                        .putBoolean("freelancer_full_profile_hint_shown", true)
                        .apply()

                }
            }
        )
    }


    private fun hideBottomNav() {
        // Animate it down (Slide out)
        binding.bottomNavigation.animate()
            .translationY(binding.bottomNavigation.height.toFloat())
            .setDuration(300)
            .withEndAction {
                binding.bottomNavigation.visibility = View.GONE
            }
            .start()
    }

    private fun showBottomNav() {
        // Make visible first
        binding.bottomNavigation.visibility = View.VISIBLE
        // Animate it up (Slide in)
        binding.bottomNavigation.animate()
            .translationY(0f)
            .setDuration(300)
            .start()
    }
}