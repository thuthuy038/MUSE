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
import com.project.models.WishlistResponse;

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

    @GET("api/orders/myorders/{userId}")
    Call<List<com.project.models.Order>> getMyOrders(
        @Path("userId") String userId
    );

    @POST("api/orders")
    Call<com.project.models.Order> createOrder(
        @Body com.project.models.Order order
    );

    @GET("api/orders/{id}")
    Call<com.project.models.Order> getOrderDetail(
        @Path("id") String orderId
    );

    @PUT("api/orders/{id}/status")
    Call<com.project.models.Order> updateOrderStatus(
        @Path("id") String orderId,
        @Body Map<String, String> status
    );

    @PUT("api/orders/{id}")
    Call<com.project.models.Order> updateOrder(
        @Path("id") String orderId,
        @Body Map<String, Object> orderData
    );

    // ========== PRODUCTS ==========
    @GET("api/products")
    Call<List<Product>> getProducts();

    @GET("api/products/{id}")
    Call<Product> getProductDetail(
        @Path("id") String productId
    );

    // ========== WISHLIST ==========
    @GET("api/users/wishlist")
    Call<List<Product>> getWishlist(
        @Header("Authorization") String token
    );

    @POST("api/users/wishlist/{productId}")
    Call<WishlistResponse> addToWishlist(
        @Header("Authorization") String token,
        @Path("productId") String productId
    );

    @DELETE("api/users/wishlist/{productId}")
    Call<WishlistResponse> removeFromWishlist(
        @Header("Authorization") String token,
        @Path("productId") String productId
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

    @POST("api/cart/merge")
    Call<ApiResponse<Void>> mergeCart(
            @Body Map<String, String> body
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
    @GET("api/vouchers")
    Call<List<Voucher>> getMyVouchers(
            @Header("Authorization") String token
    );

    @GET("api/vouchers/promotion/{promotionId}")
    Call<List<Voucher>> getVouchersByPromotion(
            @Path("promotionId") String promotionId
    );

    @POST("api/vouchers/apply")
    Call<ApplyVoucherResponse> applyVoucher(
            @Body ApplyVoucherRequest request
    );

    @GET("api/notifications/{userId}")
    Call<com.project.models.NotificationResponse> getNotifications(
            @Header("Authorization") String token,
            @Path("userId") String userId
    );

    @PUT("api/notifications/{id}/read")
    Call<com.project.models.Notification> markAsRead(
            @Header("Authorization") String token,
            @Path("id") String notificationId
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

    // ========== ADDRESS ==========
    @GET("api/users/{id}")
    Call<User> getUserById(@Path("id") String userId);

    @POST("api/address/add/{userId}")
    Call<User> addAddress(
        @Path("userId") String userId,
        @Body User.Address address
    );

    // ========== LOCATION ==========
    @GET("api/location/provinces")
    Call<List<com.project.models.Province>> getProvinces();

    @GET("api/location/districts/{code}")
    Call<List<com.project.models.District>> getDistricts(@Path("code") String provinceCode);

    @GET("api/location/wards/{code}")
    Call<List<com.project.models.Ward>> getWards(@Path("code") String districtCode);

    @GET("api/reviews/product/{productId}")
    Call<com.project.models.ReviewResponse> getProductReviews(
            @Path("productId") String productId
    );

    @GET("api/reviews/user/{userId}")
    Call<com.project.models.ReviewResponse> getUserReviews(
            @Path("userId") String userId
    );

    @POST("api/reviews")
    Call<com.project.network.ApiResponse<com.project.models.ProductReview>> postReview(
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );

    @PUT("api/reviews/{id}")
    Call<com.project.network.ApiResponse<com.project.models.ProductReview>> updateReview(
            @Path("id") String reviewId,
            @Header("Authorization") String token,
            @Body Map<String, Object> body
    );

    @retrofit2.http.Multipart
    @POST("api/upload")
    Call<Map<String, String>> uploadMedia(
        @retrofit2.http.Part okhttp3.MultipartBody.Part image
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
