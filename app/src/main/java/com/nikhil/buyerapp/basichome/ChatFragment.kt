package com.nikhil.buyerapp.basichome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.chatting.ActiveChatsAdapter
import com.nikhil.buyerapp.databinding.FragmentChatBinding
import com.nikhil.buyerapp.dataclasses.Chat

class ChatFragment : Fragment() {

    lateinit var adapter: ActiveChatsAdapter

    private var _binding: FragmentChatBinding? = null

    val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentChatBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()

        loadChats()
    }

    private fun setupRecycler() {

        adapter = ActiveChatsAdapter {
                chat,
                receiverUid,
                receiverName,
                receiverImage ->

            val bundle = Bundle().apply {

                putString("receiverUid", receiverUid)

                putString("receiverName", receiverName)

                putString("receiverImage", receiverImage)
            }

            findNavController().navigate(
                R.id.chatlist,
                bundle
            )
        }

        binding.chatlist.apply {

            adapter = this@ChatFragment.adapter

            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadChats() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        Firebase.firestore.collection("Chat")
            .whereArrayContains("participants", uid)

            .addSnapshotListener { value, error ->

                if (error != null) return@addSnapshotListener

                val chats = value?.documents?.mapNotNull {
                    it.toObject(Chat::class.java)
                }?.sortedByDescending {
                    it.lastMessageTime
                } ?: emptyList()

                if (chats.isEmpty()) {
                    adapter.submitList(emptyList())
                    return@addSnapshotListener
                }

                val userMap = mutableMapOf<String, Pair<String, String>>()
                var pending = 0

                chats.forEach { chat ->
                    val otherUserId = chat.participants.firstOrNull { it != uid }
                    if (otherUserId != null && !userMap.containsKey(otherUserId)) {
                        pending++
                        Firebase.firestore.collection("Users")
                            .document(otherUserId)
                            .get()
                            .addOnSuccessListener { userDoc ->
                                val name = userDoc.getString("fullName") ?: ""
                                val image = userDoc.getString("profilePictureUrl") ?: ""
                                userMap[otherUserId] = Pair(name, image)
                                pending--
                                // Only submit once ALL user fetches are done
                                if (pending == 0) {
                                    adapter.setUserInfo(userMap)
                                    adapter.submitList(ArrayList(chats))
                                }
                            }
                            .addOnFailureListener {
                                pending--
                                if (pending == 0) {
                                    adapter.setUserInfo(userMap)
                                    adapter.submitList(ArrayList(chats))
                                }
                            }
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}