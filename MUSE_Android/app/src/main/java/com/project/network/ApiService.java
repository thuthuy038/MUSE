package com.project.network;

import com.project.models.LoginRequest;
import com.project.models.LoginResponse;
import com.project.models.Product;
import com.project.models.RegisterRequest;
import com.project.models.RegisterResponse;
import com.project.models.User;
import com.project.models.GoogleLoginRequest;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    // ========== AUTH ==========
    @POST("api/auth/login")
    Call<LoginResponse> login(
        @Body LoginRequest request
    );

    @POST("api/auth/google")
    Call<LoginResponse> googleLogin(
        @Body GoogleLoginRequest request
    );

    @POST("api/auth/register")
    Call<RegisterResponse> register(
        @Body RegisterRequest request
    );

    @POST("api/auth/send-otp")
    Call<Map<String, String>> sendOtp(
        @Body Map<String, String> body
    );

    @POST("api/auth/reset-password")
    Call<Map<String, String>> resetPassword(
        @Body Map<String, String> body
    );

    @GET("api/auth/profile")
    Call<User> getProfile(
        @Header("Authorization") String token
    );

    // ========== PRODUCTS ==========
    @GET("api/products")
    Call<List<Product>> getProducts();

    @GET("api/products/{id}")
    Call<Product> getProductDetail(
        @Path("id") String productId
    );

    // ========== USER ==========
    @GET("api/users/{id}")
    Call<User> getUserById(
        @Path("id") String userId
    );

    @PUT("api/users/{id}")
    Call<User> updateUser(
        @Path("id") String userId,
        @Header("Authorization") String token,
        @Body Map<String, Object> userData
    );

    @GET
    Call<java.util.List<com.project.models.Province>> getProvinces(
        @retrofit2.http.Url String url
    );

    @GET
    Call<com.project.models.Province> getProvinceDetails(
        @retrofit2.http.Url String url
    );

    @GET
    Call<com.project.models.District> getDistrictDetails(
        @retrofit2.http.Url String url
    );
}
