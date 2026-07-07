package com.project.models;

public class CartRequest {
    private String userId;
    private String productId;
    private String name;
    private String image;
    private String size;
    private String color;
    private int quantity;
    private double price;

    public CartRequest(String userId, String productId, String name, String image, String size, String color, int quantity, double price) {
        this.userId = userId;
        this.productId = productId;
        this.name = name;
        this.image = image;
        this.size = size;
        this.color = color;
        this.quantity = quantity;
        this.price = price;
    }

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
    public String getName() { return name; }
    public String getImage() { return image; }
    public String getSize() { return size; }
    public String getColor() { return color; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
}
