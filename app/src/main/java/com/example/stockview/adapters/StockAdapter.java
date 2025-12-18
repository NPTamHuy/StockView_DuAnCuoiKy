package com.example.stockview.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.stockview.R;
import com.example.stockview.models.Stock;
import java.util.List;

public class StockAdapter extends RecyclerView.Adapter<StockAdapter.StockViewHolder> {
    private List<Stock> stockList;

    public StockAdapter(List<Stock> stockList) {
        this.stockList = stockList;
    }

    @NonNull
    @Override
    public StockViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_item_stock, parent, false);
        return new StockViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StockViewHolder holder, int position) {
        Stock stock = stockList.get(position);
        holder.tvTicker.setText(stock.getSymbol());
        holder.tvPrice.setText(stock.getPrice());

        String change = stock.getChangeText();
        if (change == null) change = "0.00%"; // Phòng hờ lỗi null

        holder.tvChangePercent.setText(change);

        if (change.startsWith("-")) {
            holder.tvChangePercent.setTextColor(android.graphics.Color.RED);
            holder.tvChangePercent.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.arrow_down_float, 0, 0, 0);
        } else {
            holder.tvChangePercent.setTextColor(android.graphics.Color.parseColor("#9FC131"));
            holder.tvChangePercent.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.arrow_up_float, 0, 0, 0);
        }
    }

    @Override
    public int getItemCount() {
        return stockList.size();
    }

    public static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicker, tvPrice, tvChangePercent;

        public StockViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicker = itemView.findViewById(R.id.tvTicker);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvChangePercent = itemView.findViewById(R.id.tvChangePercent);
        }
    }
}