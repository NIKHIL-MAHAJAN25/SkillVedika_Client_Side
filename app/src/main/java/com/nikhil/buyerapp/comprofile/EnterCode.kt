package com.nikhil.buyerapp.comprofile

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.nikhil.buyerapp.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.nikhil.buyerapp.basichome.hosthome

import com.nikhil.buyerapp.databinding.ActivityEnterCodeBinding
import com.nikhil.buyerapp.mailretro.ApiResponse
import com.nikhil.buyerapp.mailretro.Retromail
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.random.Random

class   EnterCode : AppCompatActivity() {
    lateinit var binding: ActivityEnterCodeBinding
    private var auth: FirebaseAuth =FirebaseAuth.getInstance()
    val auid=auth.currentUser?.uid
    val db=Firebase.firestore
    var email:String?=null;
    // at the top of the class, add these two properties
    private var countDownTimer: android.os.CountDownTimer? = null
    private var isTimerRunning = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEnterCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val boxes = listOf(
            binding.etOtp1,
            binding.etOtp2,
            binding.etOtp3,
            binding.etOtp4,
            binding.etOtp5,
            binding.etOtp6
        )
        for(i in boxes.indices)
        {
            boxes[i].addTextChangedListener(object :TextWatcher{
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int)
                {

                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if(s?.length==1 && i<boxes.size-1)
                    {
                        boxes[i+1].requestFocus()
                    }
                    else if(s.isNullOrEmpty()&& i>0)
                    {
                        boxes[i-1].requestFocus()
                    }

                }

                override fun afterTextChanged(s: Editable?) {

                }

            })
        }
        binding.btnResendCode.setOnClickListener {
            if (isTimerRunning) return@setOnClickListener

            if (auid != null) {
                generate { code ->
                    val user = mapOf("approvalCode" to code)
                    db.collection("Users")
                        .document(auid)
                        .update(user)
                        .addOnSuccessListener {
                            fetchmail(auid) { mail ->
                                if (mail != null) sendOtp(mail, code.toString())
                            }
                            Toast.makeText(this, "Security code sent on your mail", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            startResendCooldown()
        }





        binding.btnVerifyOtp.setOnClickListener {
            val code = boxes.joinToString("") { it.text.toString() }

            if (auid != null) {
                fetchcode(auid){securecode->
                    if(securecode!=null && securecode==code)
                    {
                        db.collection("Users").document(auid).update("approved",true)
                        fetchname(auid){name,location->
                            val lancer= mapOf(
                                "name" to name,
                                "uid" to auid,
                                "state" to location
                            )
                            db.collection("Client").document(auid).set(lancer, SetOptions.merge())

                        }
                        Log.d("otp","otp verified")
                        startActivity(Intent(this,hosthome::class.java))
                    }
                    else{
                        Snackbar.make(binding.root,"Invalid OTP", Snackbar.LENGTH_SHORT).show()
                        Log.d("otp", "Invalid OTP")

                    }
                }
            }
        }
    }
    fun fetchname(auid: String,onResult: (String?,String?) -> Unit)
    {
        if(auid!=null)
        {
            db.collection("Users").document(auid).get().addOnSuccessListener { document->
                if(document!=null && document.exists())
                {
                    val name = document.getString("fullName")
                    val location=document.getString("state")
                    onResult(name,location)
                }else{
                    onResult(null,null)
                }
            }
        }
    }
    fun fetchcode(auid:String,onResult: (String?)->Unit){


            db.collection("Users").document(auid).get().addOnSuccessListener {document->
                if(document!=null && document.exists()){
                    val securecode=document.get("approvalCode")
                    val securecode2 = securecode?.toString()
                    Log.d("code","Code:$securecode")
                    onResult(securecode2)
                }
                else{
                    onResult(null)
                }
            }
    }
    private fun startResendCooldown() {
        isTimerRunning = true
        binding.btnResendCode.isEnabled = false

        countDownTimer = object : android.os.CountDownTimer(30_000L, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.btnResendCode.text = "Resend in ${seconds}s"
            }
            override fun onFinish() {
                isTimerRunning = false
                binding.btnResendCode.isEnabled = true
                binding.btnResendCode.text = "Resend Code"
            }
        }.start()
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
        Retromail.instance.sendOtp(data).enqueue(object : Callback<ApiResponse>
        {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                val res=response.body()
                val msg=res?.message ?: "Unexpected response"
                Log.e("mail","mail sent")
                Toast.makeText(this@EnterCode, msg, Toast.LENGTH_SHORT).show()

            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Log.e("mail","mail not sent")

                Toast.makeText(this@EnterCode, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
            }

        })

    }
}