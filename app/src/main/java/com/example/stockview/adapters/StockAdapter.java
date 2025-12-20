package com.example.stockview.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;
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
    public void onBindViewHolder(@NonNull final StockViewHolder holder, int position) {
        final Stock stock = stockList.get(position);
        holder.tvTicker.setText(stock.getSymbol());
        holder.tvPrice.setText(stock.getPrice());

        String change = stock.getChangeText();
        if (change == null) change = "0.000%";
        holder.tvChangePercent.setText(change);

        if (change.startsWith("-")) {
            holder.tvChangePercent.setTextColor(android.graphics.Color.RED);
            holder.tvChangePercent.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.arrow_down_float, 0, 0, 0);
        } else {
            holder.tvChangePercent.setTextColor(android.graphics.Color.parseColor("#9FC131"));
            holder.tvChangePercent.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.arrow_up_float, 0, 0, 0);
        }

        if (stock.isFavorite()) {
            holder.btnFL.setImageResource(android.R.drawable.btn_star_big_on);
            holder.btnFL.setColorFilter(android.graphics.Color.parseColor("#9FC131"));
        } else {
            holder.btnFL.setImageResource(android.R.drawable.btn_star_big_off);
            holder.btnFL.setColorFilter(android.graphics.Color.GRAY);
        }

        holder.btnFL.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean newState = !stock.isFavorite();
                stock.setFavorite(newState);

                if (newState) {
                    holder.btnFL.setImageResource(android.R.drawable.btn_star_big_on);
                    holder.btnFL.setColorFilter(android.graphics.Color.parseColor("#9FC131"));
                    Toast.makeText(v.getContext(), "Đã thêm " + stock.getSymbol() + " vào Watchlist", Toast.LENGTH_SHORT).show();
                } else {
                    holder.btnFL.setImageResource(android.R.drawable.btn_star_big_off);
                    holder.btnFL.setColorFilter(android.graphics.Color.GRAY);
                    Toast.makeText(v.getContext(), "Đã xóa khỏi Watchlist", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    @Override
    public int getItemCount() {
        return stockList.size();
    }

    public static class StockViewHolder extends RecyclerView.ViewHolder {
        TextView tvTicker, tvPrice, tvChangePercent;
        ImageView btnFL;

        public StockViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTicker = itemView.findViewById(R.id.tvTicker);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvChangePercent = itemView.findViewById(R.id.tvChangePercent);
            btnFL = itemView.findViewById(R.id.btnFL);
        }
    }
}