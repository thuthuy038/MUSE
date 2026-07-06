package com.project.models;

public class CartRequest {
    private String userId;
    private String productId;
    private int quantity;
    private String color;
    private String size;
    private double price;

    public CartRequest(String userId, String productId, int quantity, String color, String size, double price) {
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.color = color;
        this.size = size;
        this.price = price;
    }

    public String getUserId() { return userId; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public String getColor() { return color; }
    public String getSize() { return size; }
    public double getPrice() { return price; }
}
