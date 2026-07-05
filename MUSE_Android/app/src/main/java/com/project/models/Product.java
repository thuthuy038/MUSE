package com.project.models;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Objects;

public class Product {

    @SerializedName("_id")
    private String _id;

    private String code;
    private String name;

    private double price;
    private Double discountPrice;
    private int discountPercent = 0;

    private int stock;
    private String description;

    private List<ProductImage> images;

    @JsonAdapter(ColorListDeserializer.class)
    @SerializedName(value = "colors", alternate = {
            "color",
            "color_list",
            "available_colors"
    })
    private List<String> colors;

    @SerializedName(value = "variants", alternate = {"sizes", "size_list"})
    private List<ProductSize> sizes;

    @SerializedName(value = "rating", alternate = {
            "ratings",
            "avgRating",
            "stars"
    })
    private double rating = 0.0;

    private int reviewCount = 0;

    @SerializedName(value = "soldCount", alternate = {
            "sold",
            "sold_count",
            "sales",
            "sold_quantity"
    })
    private int soldCount = 0;

    private String offerDescription;
    private String status;
    private String category;
    private String material;
    private boolean isNew;
    private boolean isBestSeller;
    private String sku;
    private boolean isFavorite;

    // ==========================
    // Inner Classes
    // ==========================

    public static class ProductImage {
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class ProductSize {
        private String size;
        private int quantity;

    // Local UI State
    private boolean isSelected;

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    // getter setter

        public void setSize(String size) {
            this.size = size;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
    }

    // ==========================
    // Constructors
    // ==========================

    public Product() {
    }

    public Product(String _id,
                   String code,
                   String name,
                   double price,
                   int discountPercent,
                   int stock,
                   String description,
                   double rating,
                   int reviewCount) {

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

    // ==========================
    // Getter & Setter
    // ==========================

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    // Giữ luôn getter/setter kiểu getId() để tương thích code mới
    public String getId() {
        return _id;
    }

    public void setId(String id) {
        this._id = id;
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
        if (discountPrice != null && discountPrice > 0 && discountPrice < price) {
            return discountPrice;
        }
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public Double getDiscountPrice() {
        return discountPrice;
    }

    public void setDiscountPrice(Double discountPrice) {
        this.discountPrice = discountPrice;
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

    public List<ProductImage> getImages() {
        return images;
    }

    public void setImages(List<ProductImage> images) {
        this.images = images;
    }

    public List<String> getColors() {
        return colors;
    }

    public void setColors(List<String> colors) {
        this.colors = colors;
    }

    public List<ProductSize> getSizes() {
        return sizes;
    }

    public void setSizes(List<ProductSize> sizes) {
        this.sizes = sizes;
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

    public int getSoldCount() {
        return soldCount;
    }

    public void setSoldCount(int soldCount) {
        this.soldCount = soldCount;
    }

    public String getOfferDescription() {
        return offerDescription;
    }

    public void setOfferDescription(String offerDescription) {
        this.offerDescription = offerDescription;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public boolean isBestSeller() {
        return isBestSeller;
    }

    public void setBestSeller(boolean bestSeller) {
        isBestSeller = bestSeller;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    // ==========================
    // Helper Methods
    // ==========================

    public double getOriginalPrice() {
        if (discountPrice != null && discountPrice > 0 && discountPrice < price) {
            return price;
        }

        if (discountPercent > 0) {
            return price / (1 - (discountPercent / 100.0));
        }

        return getPrice();
    }

    public String getSizeRange() {
        if (sizes == null || sizes.isEmpty()) {
            return "";
        }

        if (sizes.size() == 1) {
            return sizes.get(0).getSize();
        }

        return sizes.get(0).getSize() + "-" +
                sizes.get(sizes.size() - 1).getSize();
    }

    // ==========================
    // equals, hashCode, toString
    // ==========================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;

        Product product = (Product) o;

        return Double.compare(product.price, price) == 0 &&
                discountPercent == product.discountPercent &&
                stock == product.stock &&
                Double.compare(product.rating, rating) == 0 &&
                reviewCount == product.reviewCount &&
                soldCount == product.soldCount &&
                Objects.equals(_id, product._id) &&
                Objects.equals(code, product.code) &&
                Objects.equals(name, product.name) &&
                Objects.equals(discountPrice, product.discountPrice) &&
                Objects.equals(description, product.description) &&
                Objects.equals(images, product.images) &&
                Objects.equals(colors, product.colors) &&
                Objects.equals(sizes, product.sizes) &&
                Objects.equals(offerDescription, product.offerDescription) &&
                Objects.equals(status, product.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                _id,
                code,
                name,
                price,
                discountPrice,
                discountPercent,
                stock,
                description,
                images,
                colors,
                sizes,
                rating,
                reviewCount,
                soldCount,
                offerDescription,
                status
        );
    }

    @Override
    public String toString() {
        return "Product{" +
                "_id='" + _id + '\'' +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", discountPrice=" + discountPrice +
                ", discountPercent=" + discountPercent +
                ", stock=" + stock +
                ", description='" + description + '\'' +
                ", images=" + images +
                ", colors=" + colors +
                ", sizes=" + sizes +
                ", rating=" + rating +
                ", reviewCount=" + reviewCount +
                ", soldCount=" + soldCount +
                ", offerDescription='" + offerDescription + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
