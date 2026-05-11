package com.nikhil.buyerapp.dataclasses

import com.google.firebase.Timestamp

data class Chat (
    val chatId:String="",
    val participants:List<String> = emptyList(),
    val lastMessage: String="",
    val lastMessageTime: Timestamp?=null,
    val lastSenderId:String="",
    val unreadCount:Map<String,Int> = emptyMap()
)