package com.example.stockview;

import android.content.Intent;
import android.graphics.Color; // Cần thiết cho Color.parseColor
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.activity.EdgeToEdge;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener; // Thêm import này

import com.example.stockview.adapters.StockAdapter;
import com.example.stockview.api.ApiService;
import com.example.stockview.api.RetrofitClient;
import com.example.stockview.models.ExchangeResponse;
import com.example.stockview.models.Stock;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WatchlistActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private StockAdapter adapter;
    private List<Stock> stockList;
    private List<String> symbolsToTrack;
    private View searchLayout;
    private EditText etSearch;
    private Handler handler = new Handler();
    private double UsdVndRate = 25450.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_watchlist);

        View mainView = findViewById(R.id.main_watchlist);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, new OnApplyWindowInsetsListener() {
                @Override
                public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                }
            });
        }

        recyclerView = findViewById(R.id.rvWatchlistPage);
        searchLayout = findViewById(R.id.search_layout_watchlist);
        etSearch = findViewById(R.id.et_search_watchlist);
        ImageButton searchButton = findViewById(R.id.search_button_watchlist);
        ImageView navHome = findViewById(R.id.nav_home);
        ImageView navWatchlist = findViewById(R.id.nav_watchlist);

        if (navWatchlist != null) {
            navWatchlist.setColorFilter(Color.parseColor("#9FC131"));
        }

        symbolsToTrack = FileFL.loadWatchlist(this);
        stockList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StockAdapter(stockList);
        recyclerView.setAdapter(adapter);

        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (searchLayout.getVisibility() == View.GONE) {
                    searchLayout.setVisibility(View.VISIBLE);
                    etSearch.requestFocus();
                } else {
                    searchLayout.setVisibility(View.GONE);
                    etSearch.setText("");
                    adapter = new StockAdapter(stockList);
                    recyclerView.setAdapter(adapter);
                }
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterWatchlist(s.toString().toUpperCase().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        layGiaTriWatchlist();
    }

    private void filterWatchlist(String query) {
        List<Stock> filtered = new ArrayList<>();
        for (Stock s : stockList) {
            if (s.getSymbol().contains(query)) {
                filtered.add(s);
            }
        }
        adapter = new StockAdapter(filtered);
        recyclerView.setAdapter(adapter);
    }

    private void layGiaTriWatchlist() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        for (int i = 0; i < symbolsToTrack.size(); i++) {
            final String symbol = symbolsToTrack.get(i);
            handler.postDelayed(new Runnable() {
                @Override public void run() {
                    layGiaTriTuApi(apiService, symbol);
                }
            }, i * 1500);
        }
    }

    private void layGiaTriTuApi(ApiService apiService, final String symbol) {
        apiService.getExchangeRate("CURRENCY_EXCHANGE_RATE", symbol, "USD", "OUFBYE8GBPA336RI")
                .enqueue(new Callback<ExchangeResponse>() {
                    @Override
                    public void onResponse(Call<ExchangeResponse> call, Response<ExchangeResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ExchangeResponse.ExchangeData data = response.body().getExchangeData();
                            if (data != null) {
                                capNhatGiaoDien(data.getSymbol(), data.getPrice());
                            }
                        }
                    }
                    @Override public void onFailure(Call<ExchangeResponse> call, Throwable t) {}
                });
    }

    private void capNhatGiaoDien(String symbol, String priceUsd) {
        double rawPriceNow = Double.parseDouble(priceUsd) * UsdVndRate;
        DecimalFormat formatter = new DecimalFormat("#,###");
        final String formattedPrice = formatter.format(rawPriceNow) + " đ";

        boolean found = false;
        for (Stock item : stockList) {
            if (item.getSymbol().equalsIgnoreCase(symbol)) {
                item.setPrice(formattedPrice);
                item.setLastPrice(rawPriceNow);
                found = true;
                break;
            }
        }

        if (!found) {
            stockList.add(new Stock(symbol, formattedPrice, rawPriceNow));
        }

        runOnUiThread(new Runnable() {
            @Override public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null); // Tránh leak memory
    }
}