package com.project.models;

import java.util.Objects;

public class Product {
    private String _id;
    private String code;
    private String name;
    private double price;
    private int discountPercent = 0;
    private int stock;
    private String description;
//    private List<ProductImage> images = null;
    private double rating = 0.0;
    private int reviewCount = 0;

    public Product() {
    }

    public Product(String _id, String code, String name, double price, int discountPercent, int stock, String description, double rating, int reviewCount) {
        this._id = _id;
        this.code = code;
        this.name = name;
        this.price = price;
        this.discountPercent = discountPercent;
        this.stock = stock;
        this.description = description;
        this.rating = rating;
        this.reviewCount = reviewCount;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
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

    public int getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(int discountPercent) {
        this.discountPercent = discountPercent;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Double.compare(product.price, price) == 0 &&
                discountPercent == product.discountPercent &&
                stock == product.stock &&
                Double.compare(product.rating, rating) == 0 &&
                reviewCount == product.reviewCount &&
                Objects.equals(_id, product._id) &&
                Objects.equals(code, product.code) &&
                Objects.equals(name, product.name) &&
                Objects.equals(description, product.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id, code, name, price, discountPercent, stock, description, rating, reviewCount);
    }

    @Override
    public String toString() {
        return "Product{" +
                "_id='" + _id + '\'' +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", discountPercent=" + discountPercent +
                ", stock=" + stock +
                ", description='" + description + '\'' +
                ", rating=" + rating +
                ", reviewCount=" + reviewCount +
                '}';
    }
}
