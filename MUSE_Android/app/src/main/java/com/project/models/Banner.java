package com.project.models;

import com.google.gson.annotations.SerializedName;

public class Banner {
    @SerializedName("_id")
    private String id;
    private String title;
    private String image;
    private String status;

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getImage() { return image; }
    public String getStatus() { return status; }

    public String getImageUrl() {
        return image;
    }
}
