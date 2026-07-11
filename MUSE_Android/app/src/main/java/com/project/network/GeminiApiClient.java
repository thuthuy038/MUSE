package com.project.network;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Query;

import com.project.models.GeminiResponse;

public interface GeminiApiClient {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    Call<GeminiResponse> generateContent(
        @Query("key") String apiKey,
        @Body Map<String, Object> body
    );
}
