package com.nikhil.buyerapp.basichome

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
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
        PDFBoxResourceLoader.init(applicationContext)
        enableEdgeToEdge()
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        binding=ActivityHosthomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.host) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())

            view.setPadding(
                statusBars.left,
                statusBars.top,
                statusBars.right,
                0
            )

            insets
        }




        val bottomnav=binding.bottomNavigation.layoutParams
        bottomnav.height=resources.getDimensionPixelSize(R.dimen.bottom_nav_height)
        binding.bottomNavigation.layoutParams=bottomnav
        val navHostFragment=supportFragmentManager
            .findFragmentById(R.id.host) as NavHostFragment
        val navController:NavController=navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
        checkprof()
        // 3. Setup the BottomNavigationView with the NavController
        // (Also, use your binding variable instead of findViewById for consistency)
        val topLevelDestinations = setOf(
            R.id.home,
            R.id.chat,
            R.id.orders

        )

        // 4. Listen for navigation changes
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id in topLevelDestinations) {
                // If we are on a main screen, FORCE the bar to show
                showBottomNav()
            } else {
                // If we are on a detail screen (Post Job, Chat), hide it
                hideBottomNav()
            }
        }




    }
    private fun checkprof() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            lifecycleScope.launch {
                try {
                    val docu = db.collection("Freelancers").document(uid).get().await()
                    val ans = docu.getBoolean("profcomp") ?: false
                    if (!ans && binding.bottomNavigation.visibility == View.VISIBLE) {
                        showprofilemark()
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                }
            }
        }
    }
    private fun showprofilemark() {

        val prefs = getSharedPreferences("hints", MODE_PRIVATE)//for anti nagging
        val shownBefore = prefs.getBoolean("freelancer_full_profile_hint_shown", false)//pllay only one time
        if (shownBefore)
            return
        if (binding.bottomNavigation.visibility != View.VISIBLE) return

        binding.bottomNavigation.post{
            val menu = binding.bottomNavigation.getChildAt(0) as? ViewGroup ?: return@post//gets all the frags as a view group
            val profile = menu.childCount - 1//last index ie profile
            val target = menu.getChildAt(profile) ?: return@post//target profile
            TapTargetView.showFor(
                this@hosthome,
                TapTarget.forView(
                    target,
                    "Complete your Profile",
                    "Add your skills, experience, level and rate to start getting orders "
                )
                    .outerCircleColor(R.color.black)
                    .outerCircleAlpha(0.85f)
                    .targetCircleColor(android.R.color.white)
                    .titleTextColor(android.R.color.white)
                    .descriptionTextColor(android.R.color.white)
                    .tintTarget(true)
                    .drawShadow(true)
                    .cancelable(false)
                    .id(202)

            )

            object : TapTargetView.Listener() {
                override fun onTargetClick(view: TapTargetView) {
                    super.onTargetClick(view)
                    // Navigate to Profile tab (adjust id if needed)
                    binding.bottomNavigation.selectedItemId = R.id.profile
                    // Mark hint as shown so it won’t reappear
                    prefs.edit().putBoolean("freelancer_full_profile_hint_shown", true)
                        .apply()
                }
            }
        }
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