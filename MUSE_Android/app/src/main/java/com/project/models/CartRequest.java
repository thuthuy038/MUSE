package com.project.models;

public class CartRequest {
    private String productId;
    private int quantity;
    private String color;
    private String size;

    public CartRequest(String productId, int quantity, String color, String size) {
        this.productId = productId;
        this.quantity = quantity;
        this.color = color;
        this.size = size;
    }

    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public String getColor() { return color; }
    public String getSize() { return size; }
}
