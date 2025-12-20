package com.example.stockview;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
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
    private View searchLayout;
    private EditText etStockInput;
    private Button btnConfirm;
    private boolean isAddMode = true;
    private Handler handler = new Handler();
    private Runnable updateRunnable;
    private final int UPDATE_TIME = 60000;
    private double UsdVndRate = 25450.0;

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

        searchLayout = findViewById(R.id.search_layout);
        etStockInput = findViewById(R.id.et_stock_input);
        btnConfirm = findViewById(R.id.btn_confirm_add);
        ImageButton searchButton = findViewById(R.id.search_button);
        ImageButton addButton = findViewById(R.id.add_button);

        symbolsToTrack = FileFL.loadWatchlist(this);
        if (symbolsToTrack.isEmpty()) {
            symbolsToTrack.add("BTC");
            symbolsToTrack.add("ETH");
            FileFL.saveWatchlist(this, symbolsToTrack);
        }

        initRecyclerView();

        View.OnClickListener toggleSearchListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (searchLayout.getVisibility() == View.GONE) {
                    searchLayout.setVisibility(View.VISIBLE);
                    etStockInput.requestFocus();
                    if (v.getId() == R.id.add_button) {
                        isAddMode = true;
                        btnConfirm.setText("THÊM");
                        etStockInput.setHint("Nhập mã mới (BTC, ETH...)");
                    } else {
                        isAddMode = false;
                        btnConfirm.setText("TÌM");
                        etStockInput.setHint("Tìm trong danh sách...");
                    }
                } else {
                    searchLayout.setVisibility(View.GONE);
                    etStockInput.setText("");
                    adapter = new StockAdapter(stockList);
                    recyclerView.setAdapter(adapter);
                }
            }
        };

        searchButton.setOnClickListener(toggleSearchListener);
        addButton.setOnClickListener(toggleSearchListener);

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String symbol = etStockInput.getText().toString().toUpperCase().trim();
                if (symbol.isEmpty()) return;

                if (isAddMode) {
                    if (!symbolsToTrack.contains(symbol)) {
                        symbolsToTrack.add(symbol);
                        FileFL.saveWatchlist(MainActivity.this, symbolsToTrack);
                        layGiaTriMoiAll();
                        searchLayout.setVisibility(View.GONE);
                        etStockInput.setText("");
                    } else {
                        etStockInput.setError("Mã này đã có!");
                    }
                } else {
                    filterStockList(symbol);
                }
            }
        });

        autoUpdate();
    }

    private void filterStockList(String query) {
        List<Stock> filteredList = new ArrayList<>();
        for (Stock s : stockList) {
            if (s.getSymbol().contains(query)) {
                filteredList.add(s);
            }
        }
        adapter = new StockAdapter(filteredList);
        recyclerView.setAdapter(adapter);
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
            }, i * 1500); // Tránh rate limit 5 req/min
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
        double rawPriceNow = Double.parseDouble(priceUsd) * UsdVndRate;
        DecimalFormat formatter = new DecimalFormat("#,###");
        final String formattedPrice = formatter.format(rawPriceNow) + " đ";

        boolean exists = false;
        for (Stock item : stockList) {
            if (item.getSymbol().equalsIgnoreCase(symbol)) {
                double oldPrice = item.getLastPrice();
                if (oldPrice > 0) {
                    double percent = ((rawPriceNow - oldPrice) / oldPrice) * 100;
                    DecimalFormat df = new DecimalFormat("0.000");
                    String sign = percent > 0 ? "+" : "";
                    item.setChangeText(sign + df.format(percent) + "%");
                }
                item.setPrice(formattedPrice);
                item.setLastPrice(rawPriceNow);
                exists = true;
                break;
            }
        }

        if (!exists) {
            stockList.add(new Stock(symbol, formattedPrice, rawPriceNow));
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}