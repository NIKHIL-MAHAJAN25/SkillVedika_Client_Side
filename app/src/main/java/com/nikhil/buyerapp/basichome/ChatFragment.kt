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
import com.google.firebase.firestore.ListenerRegistration

import com.google.firebase.firestore.firestore
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.chatting.ActiveChatsAdapter
import com.nikhil.buyerapp.databinding.FragmentChatBinding
import com.nikhil.buyerapp.dataclasses.Chat

class ChatFragment : Fragment() {

    lateinit var adapter: ActiveChatsAdapter

    private var _binding: FragmentChatBinding? = null

    val binding get() = _binding!!
    private var chatsListener: ListenerRegistration? = null


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
        binding.chatShimmer.startShimmer()

        binding.chatShimmer.visibility = View.VISIBLE

        binding.chatlist.visibility = View.GONE

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

                if (error != null || _binding == null) return@addSnapshotListener
                val chats = value?.documents?.mapNotNull {
                    it.toObject(Chat::class.java)
                }?.sortedByDescending {
                    it.lastMessageTime
                } ?: emptyList()

                if (chats.isEmpty()) {
                    binding.chatShimmer.stopShimmer()
                    binding.chatShimmer.visibility = View.GONE
                    binding.chatlist.visibility = View.GONE
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmptySub.visibility = View.VISIBLE
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
                                val name = if (userDoc.exists()) {
                                    userDoc.getString("fullName") ?: "Unknown"
                                } else {
                                    "Deleted Account"
                                }
                                val image = if (userDoc.exists()) {
                                    userDoc.getString("profilePictureUrl") ?: ""
                                } else {
                                    "" // will show placeholder in Glide
                                }
                                userMap[otherUserId] = Pair(name, image)
                                pending--
                                if (pending == 0) {
                                    adapter.setUserInfo(userMap)
                                    adapter.submitList(ArrayList(chats))
                                    _binding?.let {
                                        it.chatShimmer.stopShimmer()
                                        it.chatShimmer.visibility = View.GONE
                                        it.chatlist.visibility = View.VISIBLE
                                        it.tvEmpty.visibility = View.GONE
                                        it.tvEmptySub.visibility = View.GONE
                                    }
                                }
                            }
                            .addOnFailureListener {
                                // treat fetch failure same as deleted
                                userMap[otherUserId] = Pair("Deleted Account", "")
                                pending--
                                if (pending == 0) {
                                    adapter.setUserInfo(userMap)
                                    adapter.submitList(ArrayList(chats))
                                    _binding?.let {
                                        it.chatShimmer.stopShimmer()
                                        it.chatShimmer.visibility = View.GONE
                                        it.chatlist.visibility = View.VISIBLE
                                    }
                                }
                            }
                    }
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatsListener?.remove()
        chatsListener = null

        _binding = null
    }
}