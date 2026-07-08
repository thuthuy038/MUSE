package com.project.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.project.models.Product;
import com.project.models.ProductImage;
import com.project.models.ProductImageDeserializer;
import com.project.models.ProductSizeDeserializer;
import com.project.models.Order;
import com.project.models.PaymentDeserializer;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.concurrent.TimeUnit;

public class ApiClient {
    private static final String BASE_URL = "https://server-testing-ymn9.onrender.com";

    public static final ApiClient INSTANCE = new ApiClient();

    private final ApiService apiService;

    private ApiClient() {
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
                .registerTypeAdapter(ProductImage.class, new ProductImageDeserializer())
                .registerTypeAdapter(Order.Payment.class, new PaymentDeserializer())
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        this.apiService = retrofit.create(ApiService.class);
    }

    public ApiService getInstance() {
        return apiService;
    }
}
