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
        if (sessionManager.isLoggedIn()) {
            // Call API
            String token = sessionManager.getToken();
            String userId = sessionManager.getUserId();
            double price = product.getDiscountPrice() != null && product.getDiscountPrice() > 0 
                    ? product.getDiscountPrice() : product.getPrice();
            
            CartRequest request = new CartRequest(userId, product.getId(), quantity, color, size, price);
            apiService.addToCart("Bearer " + token, request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) callback.onSuccess(null);
                    else callback.onError("Error adding to server cart");
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    callback.onError(t.getMessage());
                }
            });
        } else {
            // Save to Room
            CartItem existing = cartDao.getById(product.getId());
            if (existing != null) {
                existing.setQuantity(existing.getQuantity() + quantity);
                cartDao.update(existing);
            } else {
                String imageUrl = (product.getImages() != null && !product.getImages().isEmpty()) 
                        ? product.getImages().get(0).getUrl() : "";
                CartItem newItem = new CartItem(
                        product.getId(),
                        product.getName(),
                        product.getPrice(),
                        product.getDiscountPrice() != null ? product.getDiscountPrice() : 0,
                        imageUrl,
                        color,
                        size,
                        quantity
                );
                cartDao.insert(newItem);
            }
            callback.onSuccess(null);
        }
    }

    public void getCartItems(CartCallback<List<Product>> callback) {
        if (sessionManager.isLoggedIn()) {
            String token = sessionManager.getToken();
            apiService.getCart("Bearer " + token).enqueue(new Callback<List<Product>>() {
                @Override
                public void onResponse(Call<List<Product>> call, Response<List<Product>> response) {
                    if (response.isSuccessful()) callback.onSuccess(response.body());
                    else callback.onError("Error fetching server cart");
                }

                @Override
                public void onFailure(Call<List<Product>> call, Throwable t) {
                    callback.onError(t.getMessage());
                }
            });
        } else {
            List<CartItem> localItems = cartDao.getAll();
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
                // Wrap variant (simplified for UI)
                List<Product.ProductSize> sizes = new ArrayList<>();
                Product.ProductSize ps = new Product.ProductSize();
                ps.setSize(item.getSize());
                ps.setQuantity(item.getQuantity());
                sizes.add(ps);
                p.setSizes(sizes);

                // Add to variants for HorizontalProductAdapter display
                List<ProductVariant> variants = new ArrayList<>();
                ProductVariant pv = new ProductVariant();
                pv.setColor(item.getColor());
                pv.setSize(item.getSize());
                pv.setQuantity(item.getQuantity());
                variants.add(pv);
                p.setVariants(variants);

                // We'll need a way to pass quantity back to adapter if using Product model
                p.setQuantity(item.getQuantity());
                products.add(p);
            }
            callback.onSuccess(products);
        }
    }

    public void removeFromCart(String productId, CartCallback<Void> callback) {
        if (sessionManager.isLoggedIn()) {
            String token = sessionManager.getToken();
            apiService.removeFromCart("Bearer " + token, productId).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) callback.onSuccess(null);
                    else callback.onError("Error removing from server cart");
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    callback.onError(t.getMessage());
                }
            });
        } else {
            CartItem item = cartDao.getById(productId);
            if (item != null) {
                cartDao.delete(item);
            }
            callback.onSuccess(null);
        }
    }

    public void updateQuantity(String productId, int quantity, double price, CartCallback<Void> callback) {
        if (sessionManager.isLoggedIn()) {
            String token = sessionManager.getToken();
            String userId = sessionManager.getUserId();
            // We need color and size for API? The API definition had CartRequest.
            // Let's assume we update by productId for simplicity or find existing info.
            CartRequest request = new CartRequest(userId, productId, quantity, null, null, price);
            apiService.updateCartQuantity("Bearer " + token, request).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) callback.onSuccess(null);
                    else callback.onError("Error updating quantity on server");
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    callback.onError(t.getMessage());
                }
            });
        } else {
            CartItem item = cartDao.getById(productId);
            if (item != null) {
                item.setQuantity(quantity);
                cartDao.update(item);
            }
            callback.onSuccess(null);
        }
    }

    public void syncLocalCart() {
        if (!sessionManager.isLoggedIn()) return;

        List<CartItem> localItems = cartDao.getAll();
        if (localItems.isEmpty()) return;

        String userId = sessionManager.getUserId();
        List<CartRequest> requests = new ArrayList<>();
        for (CartItem item : localItems) {
            requests.add(new CartRequest(userId, item.getProductId(), item.getQuantity(), item.getColor(), item.getSize(), item.getDiscountPrice() > 0 ? item.getDiscountPrice() : item.getPrice()));
        }

        String token = sessionManager.getToken();
        apiService.syncCart("Bearer " + token, requests).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Cart synced successfully");
                    cartDao.deleteAll();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Failed to sync cart", t);
            }
        });
    }
}
