package com.example.stockview.models;

import com.google.gson.annotations.SerializedName;

public class ExchangeResponse {
    @SerializedName("Realtime Currency Exchange Rate")
    private ExchangeData exchangeData;

    public ExchangeData getExchangeData() { return exchangeData; }

    public static class ExchangeData {
        @SerializedName("1. From_Currency Code")
        private String symbol;

        @SerializedName("5. Exchange Rate")
        private String price;

        @SerializedName("6. Last Refreshed")
        private String lastRefreshed;

        public String getSymbol() { return symbol; }
        public String getPrice() { return price; }
        public String getLastRefreshed() { return lastRefreshed; }
    }
}