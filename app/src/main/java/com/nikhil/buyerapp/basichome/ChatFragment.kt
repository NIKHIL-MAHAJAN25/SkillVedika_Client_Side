package com.nikhil.buyerapp.basichome

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.chatting.ActiveChatsAdapter
import com.nikhil.buyerapp.databinding.FragmentChatBinding
import com.nikhil.buyerapp.databinding.FragmentChatInterfaceBinding
import com.nikhil.buyerapp.dataclasses.Chat
import kotlin.jvm.java

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ChatFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChatFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    lateinit var adapter: ActiveChatsAdapter
    private var _binding: FragmentChatBinding?=null
    val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding= FragmentChatBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        loadChats()
    }
    private fun setupRecycler() {

        adapter = ActiveChatsAdapter {

        }

        binding.chatlist.adapter = adapter

        binding.chatlist.layoutManager =
            LinearLayoutManager(requireContext())
    }
    private fun loadChats() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        Firebase.firestore.collection("Chat")
            .whereArrayContains("participants", uid).orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->

                if (error != null) return@addSnapshotListener

                val chats = value?.documents?.mapNotNull {

                    it.toObject(Chat::class.java)

                } ?: emptyList()

                adapter.submitList(chats)

                val userMap = mutableMapOf<String, Pair<String, String>>()

                chats.forEach { chat ->

                    val otherUserId = chat.participants.firstOrNull { it != uid }

                    if (otherUserId != null) {

                        Firebase.firestore.collection("Users")
                            .document(otherUserId)
                            .get()
                            .addOnSuccessListener { userDoc ->

                                val name = userDoc.getString("fullName") ?: ""

                                val image =
                                    userDoc.getString("profilePictureUrl") ?: ""

                                userMap[otherUserId] = Pair(name, image)

                                adapter.setUserInfo(userMap)
                            }
                    }
                }
            }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ChatFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ChatFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}