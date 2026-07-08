package com.project.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ProductReview {

    private String userId;

    @SerializedName(value = "customerName", alternate = {"userName", "user_name", "username", "fullName", "user.fullName", "name"})
    private String customerName;

    @SerializedName(value = "userAvatar", alternate = {"user_avatar", "avatar", "user.avatar"})
    private String userAvatar;

    private int rating;

    @SerializedName(value = "content", alternate = {"comment", "text", "description"})
    private String content;

    private String color;
    private String size;

    @SerializedName(value = "variantInfo", alternate = {"variant_info", "variant", "product_variant", "color_size"})
    private String variantInfo;

    private List<String> images;
    private String createdAt;
    private int helpfulCount;

    @SerializedName(value = "adminReply", alternate = {"shopReply", "reply"})
    private String adminReply;

    @SerializedName(value = "adminReplyAt", alternate = {"shopReplyAt", "replyAt"})
    private String adminReplyAt;

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getVariantInfo() {
        return variantInfo;
    }

    public void setVariantInfo(String variantInfo) {
        this.variantInfo = variantInfo;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getHelpfulCount() {
        return helpfulCount;
    }

    public void setHelpfulCount(int helpfulCount) {
        this.helpfulCount = helpfulCount;
    }

    public String getAdminReply() {
        return adminReply;
    }

    public void setAdminReply(String adminReply) {
        this.adminReply = adminReply;
    }

    public String getAdminReplyAt() {
        return adminReplyAt;
    }

    public void setAdminReplyAt(String adminReplyAt) {
        this.adminReplyAt = adminReplyAt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}

