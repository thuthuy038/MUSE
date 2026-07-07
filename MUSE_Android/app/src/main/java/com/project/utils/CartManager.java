package com.project.utils;

import android.content.Context;
import android.util.Log;

import com.project.database.AppDatabase;
import com.project.database.CartDao;
import com.project.database.CartItem;
import com.project.models.CartRequest;
import com.project.models.Product;
import com.project.models.ProductVariant;
import com.project.network.ApiClient;
import com.project.network.ApiService;
import com.project.network.ApiResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartManager {
    private static final String TAG = "CartManager";
    private static CartManager instance;
    private final Context context;
    private final CartDao cartDao;
    private final SessionManager sessionManager;
    private final ApiService apiService;

    private CartManager(Context context) {
        this.context = context.getApplicationContext();
        this.cartDao = AppDatabase.getInstance(this.context).cartDao();
        this.sessionManager = new SessionManager(this.context);
        this.apiService = ApiClient.INSTANCE.getInstance();
    }

    public static synchronized CartManager getInstance(Context context) {
        if (instance == null) {
            instance = new CartManager(context);
        }
        return instance;
    }

    public interface CartCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    public void addToCart(Product product, String color, String size, int quantity, CartCallback<Void> callback) {
        String activeId = sessionManager.isLoggedIn() ? sessionManager.getUserId() : sessionManager.getGuestId();
        
        String prodId = product.get_id() != null ? product.get_id() : product.getId();
        String name = product.getName();
        String imageUrl = (product.getImages() != null && !product.getImages().isEmpty()) 
                ? product.getImages().get(0).getUrl() : "";
        double price = product.getDiscountPrice() != null && product.getDiscountPrice() > 0 
                ? product.getDiscountPrice() : product.getPrice();
        
        String safeColor = color != null ? color : "";
        String safeSize = size != null ? size : "";

        Log.d(TAG, "Adding to server cart: ID=" + activeId + ", Product=" + prodId + ", Qty=" + quantity);
        
        CartRequest request = new CartRequest(activeId, prodId, name, imageUrl, safeSize, safeColor, quantity, price);
        apiService.addToCart(request).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Server cart response success");
                    updateLocalRoomAfterServer(product, safeColor, safeSize, quantity, callback);
                } else {
                    Log.e(TAG, "Server cart response error: " + response.code());
                    callback.onError("Error adding to server cart: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "Server cart request failure: " + t.getMessage());
                // Fallback to local only if server fails
                updateLocalRoomAfterServer(product, safeColor, safeSize, quantity, callback);
            }
        });
    }

    private void updateLocalRoomAfterServer(Product product, String color, String size, int quantity, CartCallback<Void> callback) {
        String prodId = product.get_id() != null ? product.get_id() : product.getId();
        String safeColor = color != null ? color : "";
        String safeSize = size != null ? size : "";
        
        new Thread(() -> {
            CartItem existing = cartDao.getByVariant(prodId, safeColor, safeSize);
            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + quantity);
                cartDao.update(existing);
            } else {
                String imageUrl = (product.getImages() != null && !product.getImages().isEmpty()) 
                        ? product.getImages().get(0).getUrl() : "";
                CartItem newItem = new CartItem(
                        prodId,
                        product.getName(),
                        product.getPrice(),
                        product.getDiscountPrice() != null ? product.getDiscountPrice() : 0,
                        imageUrl,
                        safeColor,
                        safeSize,
                        quantity
                );
                cartDao.insert(newItem);
            }
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> callback.onSuccess(null));
        }).start();
    }

    public void getCartItems(CartCallback<List<Product>> callback) {
        String activeId = sessionManager.isLoggedIn() ? sessionManager.getUserId() : sessionManager.getGuestId();
        
        new Thread(() -> {
            List<CartItem> localItems = cartDao.getAll();
            List<Product> products = convertToProducts(localItems);

            apiService.getCart(activeId).enqueue(new Callback<ApiResponse<List<Product>>>() {
                @Override
                public void onResponse(Call<ApiResponse<List<Product>>> call, Response<ApiResponse<List<Product>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Product> serverProducts = response.body().getData();
                        if (serverProducts != null) {
                            callback.onSuccess(serverProducts);
                            syncLocalCacheWithServer(serverProducts);
                        } else {
                            callback.onSuccess(products);
                        }
                    } else {
                        callback.onSuccess(products);
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<List<Product>>> call, Throwable t) {
                    callback.onSuccess(products);
                }
            });
        }).start();
    }

    public void mergeGuestCart(String userId) {
        String guestId = sessionManager.getGuestId();
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("guestId", guestId);
        body.put("userId", userId);

        apiService.mergeCart(body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Cart merged successfully");
                    // Refresh from server after merge
                    getCartItems(new CartCallback<List<Product>>() {
                        @Override
                        public void onSuccess(List<Product> result) {}
                        @Override
                        public void onError(String message) {}
                    });
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "Failed to merge cart", t);
            }
        });
    }

    private List<Product> convertToProducts(List<CartItem> localItems) {
        List<Product> products = new ArrayList<>();
        for (CartItem item : localItems) {
            Product p = new Product();
            p.setId(item.getProductId());
            p.setName(item.getName());
            p.setPrice(item.getPrice());
            p.setDiscountPrice(item.getDiscountPrice());
            // Wrap image
            List<Product.ProductImage> images = new ArrayList<>();
            Product.ProductImage img = new Product.ProductImage();
            img.setUrl(item.getImageUrl());
            images.add(img);
            p.setImages(images);

            // Add to variants for HorizontalProductAdapter display
            List<ProductVariant> variants = new ArrayList<>();
            ProductVariant pv = new ProductVariant();
            pv.setColor(item.getColor());
            pv.setSize(item.getSize());
            pv.setQuantity(item.getQuantity());
            variants.add(pv);
            p.setVariants(variants);

            p.setQuantity(item.getQuantity());
            products.add(p);
        }
        return products;
    }

    private void syncLocalCacheWithServer(List<Product> serverProducts) {
        new Thread(() -> {
            cartDao.deleteAll();
            for (Product p : serverProducts) {
                String prodId = p.getId();
                String name = p.getName();
                double price = p.getPrice();
                double discount = p.getDiscountPrice() != null ? p.getDiscountPrice() : 0;
                String img = (p.getImages() != null && !p.getImages().isEmpty()) ? p.getImages().get(0).getUrl() : "";
                
                if (p.getVariants() != null && !p.getVariants().isEmpty()) {
                    for (ProductVariant v : p.getVariants()) {
                        cartDao.insert(new CartItem(prodId, name, price, discount, img, v.getColor(), v.getSize(), v.getQuantity()));
                    }
                } else {
                    cartDao.insert(new CartItem(prodId, name, price, discount, img, "", "", p.getQuantity()));
                }
            }
        }).start();
    }

    public void removeFromCart(String productId, String size, String color, CartCallback<Void> callback) {
        String safeColor = color != null ? color : "";
        String safeSize = size != null ? size : "";
        
        if (sessionManager.isLoggedIn()) {
            String userId = sessionManager.getUserId();
            apiService.removeFromCart(userId, productId, safeSize, safeColor).enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    if (response.isSuccessful()) callback.onSuccess(null);
                    else callback.onError("Error removing from server cart");
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    callback.onError(t.getMessage());
                }
            });
        } else {
            CartItem item = cartDao.getByVariant(productId, safeColor, safeSize);
            if (item != null) {
                cartDao.delete(item);
            }
            callback.onSuccess(null);
        }
    }

    public void updateQuantity(String productId, int quantity, double price, String color, String size, CartCallback<Void> callback) {
        String safeColor = color != null ? color : "";
        String safeSize = size != null ? size : "";

        if (sessionManager.isLoggedIn()) {
            String userId = sessionManager.getUserId();
            CartRequest request = new CartRequest(userId, productId, quantity, safeColor, safeSize, price);
            apiService.updateCartQuantity(request).enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    if (response.isSuccessful()) callback.onSuccess(null);
                    else callback.onError("Error updating quantity on server");
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    callback.onError(t.getMessage());
                }
            });
        } else {
            CartItem item = cartDao.getByVariant(productId, safeColor, safeSize);
            if (item != null) {
                item.setQuantity(quantity);
                cartDao.update(item);
            }
            callback.onSuccess(null);
        }
    }

    public void syncLocalCart() {
        if (!sessionManager.isLoggedIn()) return;

        new Thread(() -> {
            List<CartItem> localItems = cartDao.getAll();
            if (localItems.isEmpty()) return;

            String userId = sessionManager.getUserId();
            List<CartRequest> requests = new ArrayList<>();
            for (CartItem item : localItems) {
                requests.add(new CartRequest(userId, item.getProductId(), item.getName(), item.getImageUrl(), item.getSize(), item.getColor(), item.getQuantity(), item.getDiscountPrice() > 0 ? item.getDiscountPrice() : item.getPrice()));
            }

            apiService.syncCart(requests).enqueue(new Callback<ApiResponse<Void>>() {
                @Override
                public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Cart synced successfully with " + localItems.size() + " items");
                        // Clear only if needed, or let getCartItems refresh it from server
                    } else {
                        Log.e(TAG, "Sync cart failed: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                    Log.e(TAG, "Failed to sync cart", t);
                }
            });
        }).start();
    }
}
