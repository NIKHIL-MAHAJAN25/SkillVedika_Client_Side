package com.nikhil.buyerapp.Login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.Signup.SignupActivity2
import com.nikhil.buyerapp.basichome.hosthome
import com.nikhil.buyerapp.comprofile.ProfileScreen1
import com.nikhil.buyerapp.comprofile.ProfileScreen2
import com.nikhil.buyerapp.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    lateinit var binding:ActivityLoginBinding
    private var auth:FirebaseAuth=FirebaseAuth.getInstance()
    val db=Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding= ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.alrsignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity2::class.java))
            finish()
        }
        binding.btnSignin.setOnClickListener {
            val aemail=binding.etmailsignin2.text.toString()
            val psswrd=binding.etpsswrdsignin2.text.toString()
            if(aemail.isEmpty() || psswrd.isEmpty())
            {
                showtoast("One of the fields is empty")
                return@setOnClickListener
            }
            else{
                auth.signInWithEmailAndPassword(aemail,psswrd).addOnSuccessListener {
                    val uid = auth.currentUser?.uid
                    if(uid!=null)
                    {
                        db.collection("Users").document(uid).get().addOnSuccessListener { document->
                            if(document.exists() && document!=null)
                            {
                                val status=document.getBoolean("profilecomplete")
                                val appstatus=document.getBoolean("approved")
                                when{
                                    !status!! && !appstatus!! ->{
                                        showtoast("Complete your profile first")
                                        startActivity(Intent(this,ProfileScreen1::class.java))
                                    }
                                    status && !appstatus!! ->{
                                        showtoast("Enter the Approval code")
                                        startActivity(Intent(this,ProfileScreen2::class.java))
                                    }
                                    status && appstatus == true ->{
                                        showtoast("Successfully logged in")
                                        startActivity(Intent(this,hosthome::class.java))
                                    }
                                }
                            }else{
                                showtoast("Account doesn't exist")
                            }

                        }
                    }
                }.addOnFailureListener {
                    showtoast("Enter the credentials correctly")
                }
            }
        }
    }
    private fun showtoast(message:String)
    {
        Snackbar.make(binding.root,message,Snackbar.LENGTH_SHORT).show()


    }
}