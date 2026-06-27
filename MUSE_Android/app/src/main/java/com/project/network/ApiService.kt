package com.project.network
import com.project.models.*

import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    // ========== AUTH ==========
    @POST("api/auth/login")
    fun login(
        @Body request: LoginRequest
    ): Call<LoginResponse>

    @POST("api/auth/register")
    fun register(
        @Body request: RegisterRequest
    ): Call<RegisterResponse>

    @GET("api/auth/profile")
    fun getProfile(
        @Header("Authorization") token: String
    ): Call<User>

    // ========== PRODUCTS ==========
    @GET("api/products")
    fun getProducts(): Call<List<Product>>

    @GET("api/products/{id}")
    fun getProductDetail(
        @Path("id") productId: String
    ): Call<Product>

    // ========== USER ==========
    @GET("api/users/{id}")
    fun getUserById(
        @Path("id") userId: String
    ): Call<User>

    @PUT("api/users/{id}")
    fun updateUser(
        @Path("id") userId: String,
        @Header("Authorization") token: String,
        @Body userData: Map<String, Any>
    ): Call<User>
}