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
import com.nikhil.buyerapp.R
import com.nikhil.buyerapp.databinding.ChatlistItemBinding
import com.nikhil.buyerapp.dataclasses.Chat


class ActiveChatsAdapter(
    private val onChatClicked: (
        Chat,
        String,
        String,
        String
    ) -> Unit
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
            val otherUserId = chat.participants.firstOrNull { it != currentUid }
            val userData = userInfoMap[otherUserId]

            // Always show the row, use fallback if data missing
            binding.root.visibility = View.VISIBLE

            val name = userData?.first ?: "Deleted Account"
            val image = userData?.second ?: ""

            binding.tvName.text = name
            binding.tvLastMessage.text = chat.lastMessage
            binding.tvTime.text = DateFormat.format("hh:mm a", chat.lastMessageTime?.toDate())

            if (image.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(image)
                    .centerCrop()
                    .into(binding.ivProfileImage)
            } else {
                // Show a generic avatar for deleted accounts
                binding.ivProfileImage.setImageResource(R.drawable.outline_person_off_24)
            }

            binding.root.setOnClickListener {
                if (otherUserId != null && name != "Deleted Account") {
                    onChatClicked(chat, otherUserId, name, image)
                }
                // do nothing if account is deleted — tapping a deleted account chat goes nowhere
            }
        }
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChatViewHolder {

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

        override fun areItemsTheSame(
            oldItem: Chat,
            newItem: Chat
        ): Boolean {

            return oldItem.chatId == newItem.chatId
        }

        override fun areContentsTheSame(
            oldItem: Chat,
            newItem: Chat
        ): Boolean {

            return oldItem.lastMessage == newItem.lastMessage &&
                    oldItem.lastSenderId == newItem.lastSenderId &&
                    oldItem.lastMessageTime == newItem.lastMessageTime
        }
    }
}