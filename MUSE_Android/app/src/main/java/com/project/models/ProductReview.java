package com.project.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ProductReview {

    private String userId;

    @SerializedName(value = "customerName", alternate = {"userName", "user_name", "username", "fullName", "user.fullName", "name"})
    private com.google.gson.JsonElement rawName;

    @SerializedName(value = "userAvatar", alternate = {"user_avatar", "avatar", "user.avatar", "image"})
    private com.google.gson.JsonElement rawAvatar;

    private int rating;

    @SerializedName(value = "content", alternate = {"comment", "text", "description", "review_text"})
    private String content;

    @SerializedName(value = "color", alternate = {"productColor", "selectedColor", "chosenColor"})
    private String color;
    
    @SerializedName(value = "size", alternate = {"productSize", "selectedSize", "chosenSize"})
    private String size;

    @SerializedName(value = "variantInfo", alternate = {"variant_info", "variant", "product_variant", "color_size", "classification", "variant_name"})
    private String variantInfo;

    private List<String> images;
    private List<String> videos;
    private String createdAt;
    private int helpfulCount;
    private boolean isLiked; // Local state for UI toggle

    public boolean isLiked() {
        return isLiked;
    }

    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    @SerializedName(value = "adminReply", alternate = {"shopReply", "reply"})
    private String adminReply;

    @SerializedName(value = "adminReplyAt", alternate = {"shopReplyAt", "replyAt"})
    private String adminReplyAt;

    @SerializedName(value = "user", alternate = {"customer", "reviewer", "user_id"})
    private com.google.gson.JsonElement rawUser;

    public static class ReviewAvatar {
        @SerializedName("url")
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public String getCustomerName() {
        if (rawName != null) {
            if (rawName.isJsonPrimitive()) return rawName.getAsString();
            if (rawName.isJsonObject()) {
                com.google.gson.JsonObject obj = rawName.getAsJsonObject();
                if (obj.has("fullName")) return obj.get("fullName").getAsString();
                if (obj.has("name")) return obj.get("name").getAsString();
            }
        }
        if (rawUser != null && rawUser.isJsonObject()) {
            com.google.gson.JsonObject userObj = rawUser.getAsJsonObject();
            if (userObj.has("fullName")) return userObj.get("fullName").getAsString();
            if (userObj.has("name")) return userObj.get("name").getAsString();
            if (userObj.has("customerName")) return userObj.get("customerName").getAsString();
        }
        return null;
    }

    public void setCustomerName(String customerName) {
        this.rawName = new com.google.gson.JsonPrimitive(customerName);
    }

    public String getUserAvatar() {
        if (rawAvatar != null) {
            if (rawAvatar.isJsonPrimitive()) return rawAvatar.getAsString();
            if (rawAvatar.isJsonObject()) {
                com.google.gson.JsonObject obj = rawAvatar.getAsJsonObject();
                if (obj.has("url")) return obj.get("url").getAsString();
            }
        }
        if (rawUser != null && rawUser.isJsonObject()) {
            com.google.gson.JsonObject userObj = rawUser.getAsJsonObject();
            if (userObj.has("avatar")) {
                com.google.gson.JsonElement av = userObj.get("avatar");
                if (av.isJsonPrimitive()) return av.getAsString();
                if (av.isJsonObject() && av.getAsJsonObject().has("url")) {
                    return av.getAsJsonObject().get("url").getAsString();
                }
            }
        }
        return null;
    }

    public void setUserAvatar(String userAvatar) {
        this.rawAvatar = new com.google.gson.JsonPrimitive(userAvatar);
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
        if (images != null) return images;
        if (reviewImages != null) {
            java.util.List<String> urls = new java.util.ArrayList<>();
            for (ReviewAvatar img : reviewImages) {
                if (img.getUrl() != null) urls.add(img.getUrl());
            }
            return urls;
        }
        return null;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    @SerializedName(value = "reviewImages", alternate = {"images_list", "images_data"})
    private List<ReviewAvatar> reviewImages;

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

