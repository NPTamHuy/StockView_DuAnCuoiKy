package com.example.stockview.api;

import com.example.stockview.models.ExchangeResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface ApiService {
    @GET("query")
    Call<ExchangeResponse> getExchangeRate(
            @Query("function") String function,
            @Query("from_currency") String from,
            @Query("to_currency") String to,
            @Query("apikey") String apiKey
    );
}