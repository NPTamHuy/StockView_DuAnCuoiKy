package com.example.stockview;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

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

    // --- Khai báo các thành phần giao diện và dữ liệu ---
    private RecyclerView recyclerView; // Danh sách hiển thị các mã chứng khoán
    private StockAdapter adapter;      // Bộ điều phối dữ liệu cho danh sách
    private List<Stock> stockList;     // Danh sách đối tượng Stock để hiển thị
    private List<String> symbolsToTrack; // Danh sách các ký hiệu mã (BTC, ETH...) nạp từ JSON
    private View searchLayout;         // Vùng chứa thanh nhập liệu (ẩn/hiện)
    private EditText etStockInput;     // Ô nhập mã chứng khoán mới hoặc tìm kiếm
    private Button btnConfirm;         // Nút xác nhận Thêm hoặc Tìm
    private boolean isAddMode = true;  // Cờ đánh dấu chế độ: True là Thêm mới, False là Tìm kiếm

    // --- Khai báo công cụ cập nhật tự động ---
    private Handler handler = new Handler(); // Công cụ điều phối các luồng chạy ngầm
    private Runnable updateRunnable;         // Tác vụ lặp lại để cập nhật giá
    private final int UPDATE_TIME = 60000;   // Khoảng thời gian tự động làm mới (60 giây)
    private double UsdVndRate = 25450.0;    // Tỷ giá mặc định ban đầu

    /**
     * Hàm onCreate: Điểm khởi đầu khi chạy ứng dụng.
     * Nhiệm vụ: Thiết lập giao diện, nạp dữ liệu từ tệp và chuẩn bị các sự kiện tương tác.
     */
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

        ImageView navWatchlist = findViewById(R.id.imageView);
        navWatchlist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, WatchlistActivity.class);
                startActivity(intent);
            }
        });

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

        // Xử lý sự kiện khi nhấn nút xác nhận (THÊM hoặc TÌM)
        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String symbol = etStockInput.getText().toString().toUpperCase().trim();
                if (symbol.isEmpty()) return;

                if (isAddMode) {
                    // Chế độ THÊM: Kiểm tra trùng lặp, lưu vào JSON và lấy giá ngay
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
                    // Lọc danh sách hiện tại
                    filterStockList(symbol);
                }
            }
        });

        autoUpdate();
    }

    /**
     * Hàm lọc danh sách hiển thị dựa trên từ khóa tìm kiếm.
     */
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

    /**
     * Hàm cấu hình RecyclerView để hiển thị danh sách chứng khoán.
     */
    private void initRecyclerView() {
        recyclerView = findViewById(R.id.rvWatchlist);
        stockList = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StockAdapter(stockList);
        recyclerView.setAdapter(adapter);
    }

    /**
     * Hàm tạo vòng lặp cập nhật dữ liệu tự động.
     * Cơ chế: Lấy tỷ giá trước -> Chờ 2 giây -> Lấy giá chứng khoán -> Lặp lại sau 60 giây.
     */
    private void autoUpdate() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                currUsdVndRate(); // Cập nhật tỷ giá mới nhất
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        layGiaTriMoiAll(); // Cập nhật giá các mã
                    }
                }, 2000);
                handler.postDelayed(this, UPDATE_TIME);
            }
        };
        handler.post(updateRunnable);
    }

    /**
     * Hàm lấy tỷ giá USD sang VND hiện tại từ API.
     */
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

    /**
     * Hàm duyệt danh sách để lấy giá cho tất cả mã đang theo dõi.
     */
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

    /**
     * Hàm gọi API lấy giá cho một mã cụ thể.
     */
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

    /**
     * Hàm xử lý logic tính toán tài chính và cập nhật giao diện.
     * Nhiệm vụ: Tính tiền VND, tính % biến động, định dạng số và báo cho Adapter làm mới UI.
     */
    private void updateStock(String symbol, String priceUsd) {
        // Quy đổi từ USD sang VND
        double rawPriceNow = Double.parseDouble(priceUsd) * UsdVndRate;
        DecimalFormat formatter = new DecimalFormat("#,###"); // Định dạng ngăn cách hàng nghìn
        final String formattedPrice = formatter.format(rawPriceNow) + " đ";

        boolean exists = false;
        for (Stock item : stockList) {
            if (item.getSymbol().equalsIgnoreCase(symbol)) {
                // Tính toán phần trăm tăng giảm dựa trên giá cũ
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

        // Nếu mã chưa có trong danh sách hiển thị thì thêm mới
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

    /**
     * Giải phóng tài nguyên khi Activity bị hủy để tránh lỗi rò rỉ bộ nhớ.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}