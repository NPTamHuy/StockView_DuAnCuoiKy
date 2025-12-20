package com.example.stockview.models;

public class Stock {
    private String symbol;
    private String price;
    private double lastPrice;
    private String changeText;
    private boolean isFavorite;

    public Stock(String symbol, String price, double lastPrice) {
        this.symbol = symbol;
        this.price = price;
        this.lastPrice = this.lastPrice;
        this.changeText = "0.000%";
    }

    public String getSymbol() { return symbol; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
    public double getLastPrice() { return lastPrice; }
    public void setLastPrice(double lastPrice) { this.lastPrice = lastPrice; }
    public String getChangeText() { return changeText; }
    public void setChangeText(String changeText) { this.changeText = changeText; }
    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}