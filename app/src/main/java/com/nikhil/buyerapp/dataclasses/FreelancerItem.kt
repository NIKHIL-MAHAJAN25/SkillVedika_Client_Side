package com.nikhil.buyerapp.dataclasses

data class FreelancerItem(
    val uid: String="",
    val name: String="",
    val primaryskill: String="",
    val rating:Double?=0.0,//basic
    val projectRate:Any?=null,//basic
    var profileImageUrl: String=""
)