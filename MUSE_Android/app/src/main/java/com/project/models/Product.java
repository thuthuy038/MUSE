package com.project.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Objects;

public class Product {
    @SerializedName("_id")
    private String id;
    private String code;
    private String name;
    private double price;
    private int discountPercent = 0;
    private int stock;
    private String description;
    private List<ProductImage> images;
    private List<String> colors;
    private List<ProductSize> sizes;
    private double rating = 0.0;
    private int reviewCount = 0;
    private int soldCount = 0;
    private String offerDescription;

    public static class ProductImage {
        private String url;
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public static class ProductSize {
        private String size;
        private int quantity;
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }

    public Product() {
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<ProductImage> getImages() { return images; }
    public void setImages(List<ProductImage> images) { this.images = images; }

    public List<String> getColors() { return colors; }
    public void setColors(List<String> colors) { this.colors = colors; }

    public List<ProductSize> getSizes() { return sizes; }
    public void setSizes(List<ProductSize> sizes) { this.sizes = sizes; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getReviewCount() { return reviewCount; }
    public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }

    public int getSoldCount() { return soldCount; }
    public void setSoldCount(int soldCount) { this.soldCount = soldCount; }

    public String getOfferDescription() { return offerDescription; }
    public void setOfferDescription(String offerDescription) { this.offerDescription = offerDescription; }

    public double getOriginalPrice() {
        if (discountPercent > 0) {
            return price / (1 - (discountPercent / 100.0));
        }
        return price;
    }

    public String getSizeRange() {
        if (sizes == null || sizes.isEmpty()) return "";
        if (sizes.size() == 1) return sizes.get(0).getSize();
        return sizes.get(0).getSize() + "-" + sizes.get(sizes.size() - 1).getSize();
    }
}
