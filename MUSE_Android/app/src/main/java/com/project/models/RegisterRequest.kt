package com.project.models

data class RegisterRequest(
    val name: String,
    val emailOrPhone: String,
    val password: String,
    val role: String = "customer"
)