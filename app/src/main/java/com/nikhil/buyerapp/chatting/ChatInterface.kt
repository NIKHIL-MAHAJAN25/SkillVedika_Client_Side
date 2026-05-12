package com.nikhil.buyerapp.chatting

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.FragmentChatInterfaceBinding
import com.nikhil.buyerapp.databinding.FragmentFreeLanceSearchBinding
import com.nikhil.buyerapp.dataclasses.Chat
import com.nikhil.buyerapp.dataclasses.Message
import com.nikhil.buyerapp.utils.snack

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class ChatInterface : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    lateinit var receiverUid:String
    lateinit var receiverName:String
    lateinit var receiverImage:String
    val db= Firebase.firestore
    var auth: FirebaseAuth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid
    private var _binding: FragmentChatInterfaceBinding?=null
    val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
            receiverUid = it.getString("receiverUid")!!
            receiverName = it.getString("receiverName")!!
            receiverImage = it.getString("receiverImage")!!

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding= FragmentChatInterfaceBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val window = requireActivity().window

        // 1. Make status bar transparent so toolbar color shows through
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        // 2. White status bar icons (since dark green bg)
        // false means light icons (for dark backgrounds), true means dark icons (for light backgrounds)
        val windowController = WindowCompat.getInsetsController(window, view)
        windowController.isAppearanceLightStatusBars = false

        // 3. Pad the toolbar by status bar height so content doesn't go under icons
        ViewCompat.setOnApplyWindowInsetsListener(binding.topHeader) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            // Explicitly set bottom padding to 0 so we don't accidentally inherit extra space
            v.updatePadding(top = statusBarHeight, bottom = 0)
            insets
        }

        // Bottom input moves with keyboard
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomInputLayout) { v, windowInsets ->
            val imeInsets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, maxOf(imeInsets.bottom, systemBars.bottom))
            windowInsets
        }

        setupinfo()
        binding.btnSend.setOnClickListener {
            if(!binding.etMessage.text.isNullOrBlank() || !binding.etMessage.text.trim().isEmpty()) {
                val text = binding.etMessage.text.trim().toString()
                sendMessage(text)

            }else{
                snack("Message is Empty")
            }
        }
    }
    private fun setupinfo()
    {
        binding.tvName.text = receiverName
        Glide.with(requireContext())
            .load(receiverImage)
            .centerCrop()
            .into(binding.ivProfileImage)

    }
    private fun sendMessage(text: String)

    {
        val currentUid = auth.currentUser?.uid ?: return
        // deterministic chat id
        val chatId = if (currentUid < receiverUid) {
            "${currentUid}_${receiverUid}"
        } else {
            "${receiverUid}_${currentUid}"
        }
        val chatref = db.collection("Chat").document(chatId)
        val messageref=chatref.collection("messages").document()
        val message = Message(
            messageId = messageref.id,
            senderId = currentUid,
            text = text,
            timestamp = com.google.firebase.Timestamp.now()

        )
        val chat = Chat(
            chatId = chatId,
            participants = listOf(currentUid, receiverUid),
            lastMessage = text,
            lastMessageTime = com.google.firebase.Timestamp.now(),
            lastSenderId = currentUid,
            unreadCount = mapOf(
                currentUid to 0,
                receiverUid to 1
            )
            )
        chatref.set(chat).continueWithTask {
                messageref.set(message)
            }

            .addOnSuccessListener {

                binding.etMessage.text?.clear()

            }

            .addOnFailureListener {

                snack("Failed to send message")

            }
    }




    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ChatInterface.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ChatInterface().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}