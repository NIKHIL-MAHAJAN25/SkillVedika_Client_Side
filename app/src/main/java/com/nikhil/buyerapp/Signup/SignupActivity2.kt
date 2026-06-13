package com.nikhil.buyerapp.Signup

import android.content.Intent
import android.os.Bundle
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
        binding=ActivitySignup2Binding.inflate(layoutInflater)
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
        binding.btnSignUP.setOnClickListener {
            val aemail=binding.etmailsignin2.text.toString()
            val password=binding.etpsswrdsignin2.text.toString()
            val confirm=binding.etpsswrdsignin3.text.toString()
            val date=FieldValue.serverTimestamp()
            if(password==confirm && aemail.isNotBlank() && password.isNotBlank() && confirm.isNotBlank()){
                auth.createUserWithEmailAndPassword(aemail,password).addOnSuccessListener {
                    val intent=Intent(this,SignupActivity2::class.java)

                    val auid=auth.currentUser?.uid
                    val user= mapOf(
                        "email" to aemail,
                        "uid" to auid,
                        "profilecomplete" to false,
                        "createdon" to date,
                        "approved" to false,
                        "userole" to UserRole.CLIENT.name

                    )
                    if(auid!=null){
                        db.collection("Users").document(auid).set(user, SetOptions.merge()).addOnSuccessListener {
                            Toast.makeText(this, "Signup successful", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, ProfileScreen1::class.java))

                        }
                    }



                }.addOnFailureListener {
                    Toast.makeText(this,"Email already Exists",Toast.LENGTH_SHORT).show()
                }
            }
            else{
                Toast.makeText(this,"Enter all the fields Correctly",Toast.LENGTH_SHORT).show()
            }
        }
    }
}