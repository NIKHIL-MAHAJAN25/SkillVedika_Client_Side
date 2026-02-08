package com.nikhil.buyerapp.dataclasses

data class Client(
    val uid: String="",
    val name:String="",
    val companyName: String? = null,
    val paymentMethods: List<String> = emptyList(),
    val reviews:List<Review> = emptyList(),//basic
    val rating:Double ?= 0.0,
    val profcomp:Boolean?=false
    )