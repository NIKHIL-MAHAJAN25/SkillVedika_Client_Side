package com.nikhil.buyerapp.chatting

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.nikhil.buyerapp.databinding.ChatlistItemBinding
import com.nikhil.buyerapp.dataclasses.Chat

class ActiveChatsAdapter(
    private val onChatClicked: (Chat) -> Unit
) : ListAdapter<Chat, ActiveChatsAdapter.ChatViewHolder>(DiffCallback()) {

    private var userInfoMap: Map<String, Pair<String, String>> = emptyMap()

    fun setUserInfo(map: Map<String, Pair<String, String>>) {
        userInfoMap = map
        notifyDataSetChanged()
    }

    inner class ChatViewHolder(
        private val binding: ChatlistItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chat: Chat) {

            val currentUid = FirebaseAuth.getInstance().currentUser?.uid

            val otherUserId = chat.participants.firstOrNull {
                it != currentUid
            }

            val userData = userInfoMap[otherUserId]

            // Prevent half-loaded UI flicker
            if (userData == null) {

                binding.root.visibility = View.INVISIBLE
                return
            }

            binding.root.visibility = View.VISIBLE

            binding.tvName.text = userData.first

            binding.tvLastMessage.text = chat.lastMessage

            binding.tvTime.text =
                DateFormat.format(
                    "hh:mm a",
                    chat.lastMessageTime?.toDate()
                )

            Glide.with(binding.root.context)
                .load(userData.second)
                .centerCrop()
                .into(binding.ivProfileImage)

            binding.root.setOnClickListener {
                onChatClicked(chat)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {

        val binding = ChatlistItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {

        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<Chat>() {

        override fun areItemsTheSame(oldItem: Chat, newItem: Chat): Boolean {

            return oldItem.chatId == newItem.chatId
        }

        override fun areContentsTheSame(oldItem: Chat, newItem: Chat): Boolean {

            return oldItem == newItem
        }
    }
}