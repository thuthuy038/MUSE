package com.project.models

data class LoginResponse(
    val _id: String,
    val name: String,
    val email: String,
    val role: String,
    val token: String
)