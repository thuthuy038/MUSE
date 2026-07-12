package com.project.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class Product implements Serializable, Parcelable {

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

    @SerializedName(value = "product_sizes", alternate = {"sizes", "size_list"})
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

    // Local UI State
    private boolean isSelected;
    private int quantity; // For Cart

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    private List<ProductVariant> variants;

    public List<ProductVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<ProductVariant> variants) {
        this.variants = variants;
    }

    // ==========================
    // Parcelable Implementation
    // ==========================

    protected Product(Parcel in) {
        _id = in.readString();
        code = in.readString();
        name = in.readString();
        price = in.readDouble();
        if (in.readByte() == 0) {
            discountPrice = null;
        } else {
            discountPrice = in.readDouble();
        }
        discountPercent = in.readInt();
        stock = in.readInt();
        description = in.readString();
        images = in.createTypedArrayList(ProductImage.CREATOR);
        colors = in.createStringArrayList();
        sizes = in.createTypedArrayList(ProductSize.CREATOR);
        rating = in.readDouble();
        reviewCount = in.readInt();
        soldCount = in.readInt();
        offerDescription = in.readString();
        status = in.readString();
        category = in.readString();
        material = in.readString();
        isNew = in.readByte() != 0;
        isBestSeller = in.readByte() != 0;
        sku = in.readString();
        isFavorite = in.readByte() != 0;
        isSelected = in.readByte() != 0;
        quantity = in.readInt();
        variants = in.createTypedArrayList(ProductVariant.CREATOR);
    }

    public static final Creator<Product> CREATOR = new Creator<Product>() {
        @Override
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        @Override
        public Product[] newArray(int size) {
            return new Product[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(_id);
        dest.writeString(code);
        dest.writeString(name);
        dest.writeDouble(price);
        if (discountPrice == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeDouble(discountPrice);
        }
        dest.writeInt(discountPercent);
        dest.writeInt(stock);
        dest.writeString(description);
        dest.writeTypedList(images);
        dest.writeStringList(colors);
        dest.writeTypedList(sizes);
        dest.writeDouble(rating);
        dest.writeInt(reviewCount);
        dest.writeInt(soldCount);
        dest.writeString(offerDescription);
        dest.writeString(status);
        dest.writeString(category);
        dest.writeString(material);
        dest.writeByte((byte) (isNew ? 1 : 0));
        dest.writeByte((byte) (isBestSeller ? 1 : 0));
        dest.writeString(sku);
        dest.writeByte((byte) (isFavorite ? 1 : 0));
        dest.writeByte((byte) (isSelected ? 1 : 0));
        dest.writeInt(quantity);
        dest.writeTypedList(variants);
    }

    // ==========================
    // Inner Classes
    // ==========================

    public static class ProductImage implements Serializable, Parcelable {
        private String url;

        public ProductImage() {
        }

        protected ProductImage(Parcel in) {
            url = in.readString();
        }

        public static final Creator<ProductImage> CREATOR = new Creator<ProductImage>() {
            @Override
            public ProductImage createFromParcel(Parcel in) {
                return new ProductImage(in);
            }

            @Override
            public ProductImage[] newArray(int size) {
                return new ProductImage[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(url);
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class ProductSize implements Serializable, Parcelable {
        private String size;
        private int quantity;

        public ProductSize() {
        }

        protected ProductSize(Parcel in) {
            size = in.readString();
            quantity = in.readInt();
        }

        public static final Creator<ProductSize> CREATOR = new Creator<ProductSize>() {
            @Override
            public ProductSize createFromParcel(Parcel in) {
                return new ProductSize(in);
            }

            @Override
            public ProductSize[] newArray(int size) {
                return new ProductSize[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(size);
            dest.writeInt(quantity);
        }

        public String getSize() {
            return size;
        }

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
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getFinalPrice() {
        if (discountPrice != null && discountPrice > 0) {
            return discountPrice;
        }
        return price;
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
        if (colors != null && !colors.isEmpty()) {
            return colors;
        }

        // Nếu mảng colors trống, tự động thu thập màu từ mảng variants
        java.util.List<String> collectedColors = new java.util.ArrayList<>();
        if (variants != null) {
            for (ProductVariant variant : variants) {
                String color = variant.getColor();
                if (color != null && !color.isEmpty() && !collectedColors.contains(color)) {
                    collectedColors.add(color);
                }
            }
        }
        return collectedColors;
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
        java.util.List<String> allSizes = new java.util.ArrayList<>();
        
        // Thu thập từ cả variants và sizes để đảm bảo hiện đủ dải size (kể cả size hết hàng)
        if (variants != null) {
            for (ProductVariant pv : variants) {
                if (pv.getSize() != null && !allSizes.contains(pv.getSize())) {
                    allSizes.add(pv.getSize());
                }
            }
        }
        
        if (sizes != null) {
            for (ProductSize ps : sizes) {
                if (ps.getSize() != null && !allSizes.contains(ps.getSize())) {
                    allSizes.add(ps.getSize());
                }
            }
        }

        if (allSizes.isEmpty()) {
            return "";
        }

        // Thống nhất logic sắp xếp giống ProductDetailActivity
        try {
            allSizes.sort((s1, s2) -> {
                try {
                    // Ưu tiên sắp xếp theo số (size giày, v.v.)
                    Double d1 = Double.parseDouble(s1.replaceAll("[^0-9.]", ""));
                    Double d2 = Double.parseDouble(s2.replaceAll("[^0-9.]", ""));
                    return d1.compareTo(d2);
                } catch (Exception e) {
                    // Logic sắp xếp cho S, M, L, XL (giống trang chi tiết)
                    String order = "XXS XS S M L XL XXL 2XL 3XL";
                    int i1 = order.indexOf(s1.toUpperCase());
                    int i2 = order.indexOf(s2.toUpperCase());
                    if (i1 != -1 && i2 != -1) return Integer.compare(i1, i2);
                    return s1.compareTo(s2);
                }
            });
        } catch (Exception ignored) {}

        if (allSizes.size() == 1) {
            return allSizes.get(0);
        }

        // Thống nhất định dạng hiển thị dải Min - Max (ví dụ: S - XL)
        return allSizes.get(0) + " - " + allSizes.get(allSizes.size() - 1);
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
