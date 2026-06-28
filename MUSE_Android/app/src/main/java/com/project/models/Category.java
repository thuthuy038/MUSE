package com.project.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Category {
    @SerializedName("_id")
    private String id;
    private String name;
    private String status;
    private List<CategoryBanner> banner;

    public static class CategoryBanner {
        private String url;
        public String getUrl() { return url; }
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public List<CategoryBanner> getBanner() { return banner; }

    public String getImageUrl() {
        if (banner != null && !banner.isEmpty()) {
            return banner.get(0).getUrl();
        }
        return null;
    }
}
