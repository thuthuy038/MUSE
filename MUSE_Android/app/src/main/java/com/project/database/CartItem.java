package com.project.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(tableName = "cart_items", primaryKeys = {"productId", "color", "size"})
public class CartItem {
    @NonNull
    private String productId;
    private String name;
    private double price;
    private double discountPrice;
    private String imageUrl;
    @NonNull
    private String color;
    @NonNull
    private String size;
    private int quantity;

    public CartItem() {
        this.productId = "";
        this.color = "";
        this.size = "";
    }

    public CartItem(@NonNull String productId, String name, double price, double discountPrice, String imageUrl, @NonNull String color, @NonNull String size, int quantity) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.discountPrice = discountPrice;
        this.imageUrl = imageUrl;
        this.color = color;
        this.size = size;
        this.quantity = quantity;
    }

    @NonNull
    public String getProductId() {
        return productId;
    }

    public void setProductId(@NonNull String productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(double discountPrice) {
        this.discountPrice = discountPrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @NonNull
    public String getColor() {
        return color;
    }

    public void setColor(@NonNull String color) {
        this.color = color;
    }

    @NonNull
    public String getSize() {
        return size;
    }

    public void setSize(@NonNull String size) {
        this.size = size;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
