package com.example.stockview;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private StockAdapter adapter;
    private List<Stock> stockList;
    private List<String> symbolsToTrack;
    private Handler handler = new Handler();
    private Runnable updateRunnable;
    private final int UPDATE_TIME = 60000;

    private double UsdVndRate = 25450.0;
    private java.util.Map<String, String> changeMap = new java.util.HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View mainView = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, new OnApplyWindowInsetsListener() {
            @Override
            public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            }
        });

        symbolsToTrack = new ArrayList<>();
        symbolsToTrack.add("BTC");
        symbolsToTrack.add("ETH");

        initRecyclerView();

        ImageButton addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addStock();
            }
        });

        autoUpdate();
    }

    private void initRecyclerView() {
        recyclerView = findViewById(R.id.rvWatchlist);
        stockList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StockAdapter(stockList);
        recyclerView.setAdapter(adapter);
    }

    private void autoUpdate() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                currUsdVndRate();

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        layGiaTriMoiAll();
                    }
                }, 2000);

                handler.postDelayed(this, UPDATE_TIME);
            }
        };
        handler.post(updateRunnable);
    }

    private void currUsdVndRate() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        apiService.getExchangeRate("CURRENCY_EXCHANGE_RATE", "USD", "VND", "OUFBYE8GBPA336RI")
                .enqueue(new Callback<ExchangeResponse>() {
                    @Override
                    public void onResponse(Call<ExchangeResponse> call, Response<ExchangeResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String rateStr = response.body().getExchangeData().getPrice();
                            UsdVndRate = Double.parseDouble(rateStr);
                            Log.d("API_RATE", "New rate: " + UsdVndRate);
                        }
                    }
                    @Override public void onFailure(Call<ExchangeResponse> call, Throwable t) {}
                });
    }

    private void layGiaTriMoiAll() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        for (int i = 0; i < symbolsToTrack.size(); i++) {
            final String symbol = symbolsToTrack.get(i);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    layGiaTriMoi(apiService, symbol);
                }
            }, i * 1500);
        }
    }

    private void layGiaTriMoi(ApiService apiService, final String symbol) {
        apiService.getExchangeRate("CURRENCY_EXCHANGE_RATE", symbol, "USD", "OUFBYE8GBPA336RI")
                .enqueue(new Callback<ExchangeResponse>() {
                    @Override
                    public void onResponse(Call<ExchangeResponse> call, Response<ExchangeResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ExchangeResponse.ExchangeData data = response.body().getExchangeData();
                            if (data != null) {
                                updateStock(data.getSymbol(), data.getPrice());
                            }
                        }
                    }
                    @Override public void onFailure(Call<ExchangeResponse> call, Throwable t) {}
                });
    }

    private void updateStock(String symbol, String priceUsd) {
        double rawPriceUsd = Double.parseDouble(priceUsd);
        double priceVndNow = rawPriceUsd * UsdVndRate;

        DecimalFormat formatter = new DecimalFormat("#,###");
        final String formattedPrice = formatter.format(priceVndNow) + " đ";

        boolean exists = false;
        for (Stock item : stockList) {
            if (item.getSymbol().equalsIgnoreCase(symbol)) {
                double oldPrice = item.getLastPrice();

                Log.d("API_CHECK", symbol + " New: " + String.format("%.2f", priceVndNow) + " | Old: " + String.format("%.2f", oldPrice));

                if (oldPrice > 0) {
                    double diff = priceVndNow - oldPrice;
                    double percent = (diff / oldPrice) * 100;

                    DecimalFormat df = new DecimalFormat("0.000");

                    String sign = percent > 0 ? "+" : "";
                    item.setChangeText(sign + df.format(percent) + "%");
                }

                item.setPrice(formattedPrice);
                item.setLastPrice(priceVndNow);
                exists = true;
                break;
            }
        }

        if (!exists) {
            stockList.add(new Stock(symbol, formattedPrice, priceVndNow));
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }
    private void addStock() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Stock");
        final EditText input = new EditText(this);
        input.setHint("Enter Symbol (BTC, ETH, etc.)");
        builder.setView(input);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String symbol = input.getText().toString().toUpperCase().trim();
                if (!symbol.isEmpty() && !symbolsToTrack.contains(symbol)) {
                    symbolsToTrack.add(symbol);
                    layGiaTriMoiAll();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}