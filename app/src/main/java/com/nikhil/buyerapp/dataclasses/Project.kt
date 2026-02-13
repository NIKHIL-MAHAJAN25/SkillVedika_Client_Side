package com.nikhil.buyerapp.dataclasses

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.sql.Timestamp

data class Project (

    val projectid:String="",
    val clientuid:String="",
    val clientName:String="",
    val freeuid:String="",
    val freename:String?="Not Assigned",
    val title: String = "",
    val description: String = "",
    val category:String="",
    val budget:Double=0.0,
    val requiredSkills: List<String> = emptyList(),
    val status: String = ProjectStatus.OPEN.name,
//    @ServerTimestamp
//    val postedAt: Timestamp? = null,  // When Client clicked "Post"
//    val startedAt: Timestamp? = null, // When Freelancer clicked "Accept"
//    val completedAt: Timestamp? = null // When Client clicked "Approve"
)
enum class ProjectStatus {
    OPEN,
    ASSIGNED,
    COMPLETED,
    CANCELLED
}