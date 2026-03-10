package com.nikhil.buyerapp.dataclasses

import com.google.firebase.Timestamp

data class Message (
    val messageId:String="",
    val senderId:String="",
    val text:String="",
    val timestamp: Timestamp?=null

)