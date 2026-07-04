package com.project.network;

import com.project.models.Banner;
import com.project.models.Category;
import com.project.models.Product;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface HomeApiService {
    @GET("api/categories")
    Call<List<Category>> getCategories();

    @GET("api/banners")
    Call<List<Banner>> getBanners();

    @GET("api/search/popular")
    Call<ApiResponse<List<String>>> getPopularSearches();

    @GET("api/products")
    Call<List<Product>> searchProducts(@Query("q") String query);
}
