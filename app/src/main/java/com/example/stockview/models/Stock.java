package com.example.stockview.models;

public class Stock {
    private String symbol;
    private String price;
    private double lastPrice;
    private String changeText;

    public Stock(String symbol, String price, double lastPrice) {
        this.symbol = symbol;
        this.price = price;
        this.lastPrice = this.lastPrice;
        this.changeText = "0.00%";
    }

    // Getter và Setter
    public String getSymbol() { return symbol; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public double getLastPrice() { return lastPrice; }
    public void setLastPrice(double lastPrice) { this.lastPrice = lastPrice; }
    public String getChangeText() { return changeText; }
    public void setChangeText(String changeText) { this.changeText = changeText; }
}