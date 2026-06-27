package com.project.models

data class User(
    val _id: String,
    val code: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val role: String,
    val avatar: String? = null
)