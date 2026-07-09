package com.project.models;

import com.google.gson.annotations.SerializedName;

public class WishlistResponse {
    @SerializedName("message")
    private String message;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
