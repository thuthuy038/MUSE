package com.project.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Model
 */
public class Order implements Serializable {
    @SerializedName("_id")
    private String _id;
    private String id; // ORD-xxx
    private String userId;
    private String customerName;
    private String email;
    private String phone;
    private String address;
    private List<OrderItem> items;
    private double totalPrice;
    private double discount;
    private double finalPrice;
    private String status;
    private String note;
    private String paymentMethod;
    private String paymentStatus;
    private String voucherCode;
    private Payment paymentId;
    private String createdAt;
    private String updatedAt;

    // Cancellation info
    private String cancellationReason;
    private String cancelledBy;
    private String cancelledAt;
    private boolean isReviewed;

    // Return/refund info
    private String returnEmail;
    private String returnReason;
    private String returnMethod;
    private String returnNote;
    private List<String> returnMedia;
    private String returnRequestedAt;
    private String returnProcessedAt;

    public boolean isReviewed() {
        return isReviewed;
    }

    public void setReviewed(boolean reviewed) {
        isReviewed = reviewed;
    }

    // MongoDB schema support fields
    private ShippingAddress shippingAddress;
    private ShippingMethod shippingMethod;
    private Promotion promotion;
    private double subTotal;

    // Nested classes matching MongoDB schema
    public static class ShippingAddress implements Serializable {
        private String fullName;
        private String email;
        private String phone;
        private String address;
        private String city;
        private String district;
        private String ward;

        public ShippingAddress() {}

        public ShippingAddress(String fullName, String email, String phone, String address, String city, String district, String ward) {
            this.fullName = fullName;
            this.email = email;
            this.phone = phone;
            this.address = address;
            this.city = city;
            this.district = district;
            this.ward = ward;
        }

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }

        public String getWard() { return ward; }
        public void setWard(String ward) { this.ward = ward; }
    }

    public static class ShippingMethod implements Serializable {
        private String name;
        private double fee;

        public ShippingMethod() {}

        public ShippingMethod(String name, double fee) {
            this.name = name;
            this.fee = fee;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public double getFee() { return fee; }
        public void setFee(double fee) { this.fee = fee; }
    }

    public static class Promotion implements Serializable {
        private double discountAmount;
        private String code;

        public Promotion() {}

        public Promotion(double discountAmount, String code) {
            this.discountAmount = discountAmount;
            this.code = code;
        }

        public double getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class Payment implements Serializable {
        private String _id;
        private String paymentMethod;
        private String paymentStatus;
        private double amount;
        private String transactionId;
        private String paymentDate;

        public String get_id() { return _id; }
        public void set_id(String _id) { this._id = _id; }

        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

        public String getPaymentStatus() { return paymentStatus; }
        public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

        public double getAmount() { return amount; }
        public void setAmount(double amount) { this.amount = amount; }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

        public String getPaymentDate() { return paymentDate; }
        public void setPaymentDate(String paymentDate) { this.paymentDate = paymentDate; }

        public boolean isPaid() {
            return "Đã thanh toán".equals(paymentStatus);
        }
    }

    public Order() {}

    // Inner class for items
    public static class OrderItem implements Serializable {
        private String productId;
        private String name;
        private String image;
        private String size;
        private String color;
        private int quantity;
        private double price;

        public OrderItem() {}

        public OrderItem(String productId, String name, String image, String size, String color, int quantity, double price) {
            this.productId = productId;
            this.name = name;
            this.image = image;
            this.size = size;
            this.color = color;
            this.quantity = quantity;
            this.price = price;
        }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }

        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }

        // Convert to Product for compatibility with existing adapters
        public Product toProduct() {
            Product p = new Product();
            p.setId(productId);
            p.setName(name);
            p.setPrice(price);
            p.setQuantity(quantity);
            
            List<Product.ProductImage> images = new ArrayList<>();
            if (image != null && !image.isEmpty()) {
                Product.ProductImage img = new Product.ProductImage();
                img.setUrl(image);
                images.add(img);
            }
            p.setImages(images);

            List<ProductVariant> variants = new ArrayList<>();
            if ((color != null && !color.isEmpty()) || (size != null && !size.isEmpty())) {
                ProductVariant variant = new ProductVariant();
                variant.setColor(color != null ? color : "");
                variant.setSize(size != null ? size : "");
                variants.add(variant);
            }
            p.setVariants(variants);

            return p;
        }
    }

    // Compatibility method for existing code that uses getProducts()
    public List<Product> getProducts() {
        List<Product> products = new ArrayList<>();
        if (items != null) {
            for (OrderItem item : items) {
                products.add(item.toProduct());
            }
        }
        return products;
    }

    // Getters and Setters
    public String get_id() { return _id; }
    public void set_id(String _id) { this._id = _id; }

    public String getId() { return id != null ? id : _id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { 
        return status != null ? status : "PENDING"; 
    }
    public void setStatus(String status) { this.status = status; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCustomerName() {
        if (shippingAddress != null && shippingAddress.getFullName() != null) {
            return shippingAddress.getFullName();
        }
        return customerName;
    }
    public void setCustomerName(String customerName) {
        this.customerName = customerName;
        if (shippingAddress == null) shippingAddress = new ShippingAddress();
        shippingAddress.setFullName(customerName);
    }

    public String getEmail() {
        if (shippingAddress != null && shippingAddress.getEmail() != null) {
            return shippingAddress.getEmail();
        }
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
        if (shippingAddress == null) shippingAddress = new ShippingAddress();
        shippingAddress.setEmail(email);
    }

    public String getPhone() {
        if (shippingAddress != null && shippingAddress.getPhone() != null) {
            return shippingAddress.getPhone();
        }
        return phone;
    }
    public void setPhone(String phone) {
        this.phone = phone;
        if (shippingAddress == null) shippingAddress = new ShippingAddress();
        shippingAddress.setPhone(phone);
    }

    public String getAddress() {
        if (shippingAddress != null && shippingAddress.getAddress() != null) {
            return shippingAddress.getAddress();
        }
        return address;
    }
    public void setAddress(String address) {
        this.address = address;
        if (shippingAddress == null) shippingAddress = new ShippingAddress();
        shippingAddress.setAddress(address);
    }

    public double getDiscount() {
        if (promotion != null) {
            return promotion.getDiscountAmount();
        }
        return discount;
    }
    public void setDiscount(double discount) {
        this.discount = discount;
        if (promotion == null) promotion = new Promotion();
        promotion.setDiscountAmount(discount);
    }

    public double getFinalPrice() {
        if (finalPrice > 0) {
            return finalPrice;
        }
        return totalPrice;
    }
    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
        this.totalPrice = finalPrice; // Sync with totalPrice for safety
    }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }

    public Payment getPaymentId() { return paymentId; }
    public void setPaymentId(Payment paymentId) { this.paymentId = paymentId; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }

    public String getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(String cancelledAt) { this.cancelledAt = cancelledAt; }

    // New getters and setters for MongoDB Schema
    public ShippingAddress getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(ShippingAddress shippingAddress) { this.shippingAddress = shippingAddress; }

    public ShippingMethod getShippingMethod() { return shippingMethod; }
    public void setShippingMethod(ShippingMethod shippingMethod) { this.shippingMethod = shippingMethod; }

    public Promotion getPromotion() { return promotion; }
    public void setPromotion(Promotion promotion) { this.promotion = promotion; }

    public double getSubTotal() {
        if (subTotal > 0) {
            return subTotal;
        }
        // Fallback: if subTotal is 0, it might be an older model where totalPrice was used as subtotal
        return totalPrice;
    }
    public void setSubTotal(double subTotal) { this.subTotal = subTotal; }

    public String getReturnEmail() { return returnEmail; }
    public void setReturnEmail(String returnEmail) { this.returnEmail = returnEmail; }

    public String getReturnReason() { return returnReason; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }

    public String getReturnMethod() { return returnMethod; }
    public void setReturnMethod(String returnMethod) { this.returnMethod = returnMethod; }

    public String getReturnNote() { return returnNote; }
    public void setReturnNote(String returnNote) { this.returnNote = returnNote; }

    public List<String> getReturnMedia() { return returnMedia; }
    public void setReturnMedia(List<String> returnMedia) { this.returnMedia = returnMedia; }

    public String getReturnRequestedAt() { return returnRequestedAt; }
    public void setReturnRequestedAt(String returnRequestedAt) { this.returnRequestedAt = returnRequestedAt; }

    public String getReturnProcessedAt() { return returnProcessedAt; }
    public void setReturnProcessedAt(String returnProcessedAt) { this.returnProcessedAt = returnProcessedAt; }
}
