package com.nikhil.buyerapp.comprofile

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.chip.Chip
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.firestore
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.ActivityProfileScreen2Binding
import com.nikhil.buyerapp.mailretro.ApiResponse
import com.nikhil.buyerapp.mailretro.Retromail
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class ProfileScreen2 : AppCompatActivity() {
    lateinit var binding:ActivityProfileScreen2Binding
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()


    private val languages = listOf("Telugu",
        "English",
        "Assamese",
        "Konkani",
        "Dogri",
        "Tulu",
        "Gujarati",
        "Kannada",
        "Malayalam",
        "Marathi",
        "Manipuri",
        "Mizo",
        "Odia",
        "Punjabi",
        "Nepali",
        "Tamil",
        "Bengali",
        "Urdu",
        "Hindi",
        "Ladakhi")
    val db=Firebase.firestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding=ActivityProfileScreen2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        populate()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.btnNext.setOnClickListener {
            val biotext = binding.etBio.text.toString()
            if (biotext.isEmpty()) {
                Toast.makeText(this, "Please enter your bio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val selected = mutableListOf<String>()
            for (i in 0 until binding.chipGroupLang.childCount) {
                val chip = binding.chipGroupLang.getChildAt(i) as Chip
                if (chip.isChecked) {
                    selected.add(chip.text.toString())
                }
            }
            if (selected.isEmpty()) {
                Toast.makeText(this, "Please select at least one language", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            savedata(biotext,selected)
            startActivity(Intent(this,EnterCode::class.java))
            finish()
        }
    }
    private fun savedata(biotext:String, selected:List<String>) {

        generate { code ->
            val user = mapOf(
                "bio" to biotext,
                "language" to selected,
                "profilecomplete" to true,
                "approved" to false,
                "approvalCode" to code
            )
            val uid = auth.currentUser?.uid

            if (uid != null) {
                db.collection("Users").document(uid).update(user).addOnSuccessListener {
                    fetchmail(uid){mail->
                        if (mail != null) {
                            sendOtp(mail,code.toString())
                        }
                    }
                    Toast.makeText(this, "Profile updated! Security code sent Check your mail", Toast.LENGTH_SHORT).show()
                }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun populate() {
        languages.forEach { language ->
            val chip = Chip(this, null, R.style.CustomChipStyle).apply {
                text = language
                isCheckable = true
                isClickable = true
                textAlignment = android.view.View.TEXT_ALIGNMENT_CENTER
                setPadding(32, 16, 32, 16)

                // Force background color — Material3 ignores XML chipBackgroundColor via constructor
                val bgColor = ContextCompat.getColor(context, R.color.bg)
                chipBackgroundColor = ColorStateList.valueOf(bgColor)
                setTextColor(ContextCompat.getColor(context, R.color.black))
            }
            binding.chipGroupLang.addView(chip)

        }
    }
    private fun fetchmail(uid:String,onResult: (String?) -> Unit)
    {
        if(uid!=null)
        {
            db.collection("Users").document(uid).get().addOnSuccessListener { document->
                if(document.exists() && document!=null)
                {
                    val number=document.getString("email")
                    onResult(number)
                }
            }
        }else{
            onResult(null)
        }
    }

    private fun generate(onResult: (Int?)->Unit)
    {
        val random= Random.nextInt(100000,999999)
        onResult(random)
    }
    fun sendOtp(email:String,otp:String)
    {
        val data= mapOf("email" to email,"otp" to otp)
        Retromail.instance.sendOtp(data).enqueue(object :Callback<ApiResponse>
        {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
               val res=response.body()
                val msg=res?.message ?: "Unexpected response"
                Log.e("mail","mail sent")
                Toast.makeText(this@ProfileScreen2, msg, Toast.LENGTH_SHORT).show()

            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("mail","mail not sent")

                Toast.makeText(this@ProfileScreen2, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }

        })

    }

}