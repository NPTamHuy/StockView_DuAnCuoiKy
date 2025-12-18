package com.example.stockview.models;

public class Stock {
    private String symbol;
    private String price;

    public Stock(String symbol, String price) {
        this.symbol = symbol;
        this.price = price;
    }

    public String getSymbol() { return symbol; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
}