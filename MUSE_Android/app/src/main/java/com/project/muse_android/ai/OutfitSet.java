package com.project.muse_android.ai;

import com.project.models.Product;

public class OutfitSet {
    private String name;
    private Product top;
    private Product bottom;
    private String description;

    public OutfitSet(String name, Product top, Product bottom, String description) {
        this.name = name;
        this.top = top;
        this.bottom = bottom;
        this.description = description;
    }

    public String getName() { return name; }
    public Product getTop() { return top; }
    public Product getBottom() { return bottom; }
    public String getDescription() { return description; }
}
