package com.project.utils;

import android.content.Context;
import android.util.Log;

import com.project.models.Product;
import com.project.models.WishlistResponse;
import com.project.network.ApiService;
import com.project.network.HomeApiClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WishlistManager {
    private static final String TAG = "WishlistManager";
    private static WishlistManager instance;
    private final ApiService apiService;
    private final SessionManager sessionManager;

    private WishlistManager(Context context) {
        this.apiService = HomeApiClient.getApiService();
        this.sessionManager = new SessionManager(context);
    }

    public static synchronized WishlistManager getInstance(Context context) {
        if (instance == null) {
            instance = new WishlistManager(context);
        }
        return instance;
    }

    public interface WishlistCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    // =============================================
    // 1. LẤY DANH SÁCH WISHLIST
    // =============================================
    public void getWishlist(WishlistCallback<List<Product>> callback) {
        String token = sessionManager.getToken();
        if (token == null) {
            callback.onError("Vui lòng đăng nhập");
            return;
        }

        apiService.getWishlist("Bearer " + token).enqueue(new Callback<List<Product>>() {
            @Override
            public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Không thể lấy danh sách yêu thích");
                }
            }

            @Override
            public void onFailure(Call<List<Product>> call, Throwable t) {
                Log.e(TAG, "getWishlist error: " + t.getMessage());
                callback.onError(t.getMessage());
            }
        });
    }

    // =============================================
    // 2. THÊM SẢN PHẨM VÀO WISHLIST
    // =============================================
    public void addToWishlist(String productId, WishlistCallback<WishlistResponse> callback) {
        String token = sessionManager.getToken();
        if (token == null) {
            callback.onError("Vui lòng đăng nhập");
            return;
        }

        apiService.addToWishlist("Bearer " + token, productId).enqueue(new Callback<WishlistResponse>() {
            @Override
            public void onResponse(Call<WishlistResponse> call, Response<WishlistResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Không thể thêm vào danh sách yêu thích");
                }
            }

            @Override
            public void onFailure(Call<WishlistResponse> call, Throwable t) {
                Log.e(TAG, "addToWishlist error: " + t.getMessage());
                callback.onError(t.getMessage());
            }
        });
    }

    // =============================================
    // 3. XÓA SẢN PHẨM KHỎI WISHLIST
    // =============================================
    public void removeFromWishlist(String productId, WishlistCallback<WishlistResponse> callback) {
        String token = sessionManager.getToken();
        if (token == null) {
            callback.onError("Vui lòng đăng nhập");
            return;
        }

        apiService.removeFromWishlist("Bearer " + token, productId).enqueue(new Callback<WishlistResponse>() {
            @Override
            public void onResponse(Call<WishlistResponse> call, Response<WishlistResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Không thể xóa khỏi danh sách yêu thích");
                }
            }

            @Override
            public void onFailure(Call<WishlistResponse> call, Throwable t) {
                Log.e(TAG, "removeFromWishlist error: " + t.getMessage());
                callback.onError(t.getMessage());
            }
        });
    }

    // =============================================
    // 4. KIỂM TRA SẢN PHẨM TRONG WISHLIST
    // =============================================
    public void isInWishlist(String productId, WishlistCallback<Boolean> callback) {
        getWishlist(new WishlistCallback<List<Product>>() {
            @Override
            public void onSuccess(List<Product> wishlist) {
                boolean found = false;
                for (Product product : wishlist) {
                    String id = product.get_id() != null ? product.get_id() : product.getId();
                    if (id.equals(productId)) {
                        found = true;
                        break;
                    }
                }
                callback.onSuccess(found);
            }

            @Override
            public void onError(String message) {
                callback.onError(message);
            }
        });
    }
}
