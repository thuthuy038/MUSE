package com.project.network;

import com.project.models.ProductImageDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.project.models.Product;
import com.project.models.ProductSizeDeserializer;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class HomeApiClient {
    private static final String BASE_URL = "https://server-testing-ymn9.onrender.com";
    private static Retrofit retrofit = null;

    public static HomeApiService getHomeApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Product.ProductSize.class, new ProductSizeDeserializer())
                    .registerTypeAdapter(Product.ProductImage.class, new ProductImageDeserializer())
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit.create(HomeApiService.class);
    }
}
