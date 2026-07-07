package com.project.network;

import com.project.models.LoginRequest;
import com.project.models.LoginResponse;
import com.project.models.Product;
import com.project.models.RegisterRequest;
import com.project.models.RegisterResponse;
import com.project.models.User;
import com.project.models.GoogleLoginRequest;
import com.project.models.Promotion;
import com.project.models.Voucher;
import com.project.models.ApplyVoucherRequest;
import com.project.models.ApplyVoucherResponse;
import com.project.models.CartRequest;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
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

    @GET("api/orders/user")
    Call<List<com.project.models.Order>> getOrders(
        @Header("Authorization") String token
    );

    // ========== PRODUCTS ==========
    @GET("api/products")
    Call<List<Product>> getProducts();

    @GET("api/products/{id}")
    Call<Product> getProductDetail(
        @Path("id") String productId
    );

    // ========== CART ==========
    @GET("api/cart/{userId}")
    Call<ApiResponse<List<Product>>> getCart(
            @Path("userId") String userId
    );

    @POST("api/cart/add")
    Call<ApiResponse<Void>> addToCart(
            @Body CartRequest request
    );

    @PUT("api/cart/update-quantity")
    Call<ApiResponse<Void>> updateCartQuantity(
            @Body CartRequest request
    );

    @DELETE("api/cart/remove/{userId}/{productId}/{size}/{color}")
    Call<ApiResponse<Void>> removeFromCart(
            @Path("userId") String userId,
            @Path("productId") String productId,
            @Path("size") String size,
            @Path("color") String color
    );

    @POST("api/cart/sync")
    Call<ApiResponse<Void>> syncCart(
            @Body List<CartRequest> requests
    );

    // ======================
    // PROMOTION
    // ======================
    @GET("api/promotions")
    Call<List<Promotion>> getPromotions();

    @GET("api/promotions/{id}")
    Call<Promotion> getPromotionById(
            @Path("id") String promotionId
    );

    // ======================
    // VOUCHER
    // ======================
    @GET("api/vouchers/promotion/{promotionId}")
    Call<List<Voucher>> getVouchersByPromotion(
            @Path("promotionId") String promotionId
    );

    @POST("api/vouchers/apply")
    Call<ApplyVoucherResponse> applyVoucher(
            @Body ApplyVoucherRequest request
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

    @retrofit2.http.Multipart
    @POST("api/users/{id}/avatar")
    Call<User> uploadAvatar(
        @Path("id") String userId,
        @retrofit2.http.Part okhttp3.MultipartBody.Part avatar
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
