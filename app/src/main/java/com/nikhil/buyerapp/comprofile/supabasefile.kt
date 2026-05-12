package com.nikhil.buyerapp.comprofile
import com.nikhil.buyerapp.BuildConfig
import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

class supabasefile: Application() {
    lateinit var supabaseClient: SupabaseClient
    override fun onCreate() {
        super.onCreate()
        FirebaseFirestore.getInstance().firestoreSettings =
            firestoreSettings {
                isPersistenceEnabled = true
            }
        supabaseClient= createSupabaseClient(
            BuildConfig.SUPABASE_URL,
            BuildConfig.SUPABASE_KEY
        ){
            install(Storage)
        }
        // Now you can access Supabase Auth and Storage through the `supabaseClient`
//        val auth = supabaseClient.auth
        val bucket = supabaseClient.storage.from("sample")
    }
}