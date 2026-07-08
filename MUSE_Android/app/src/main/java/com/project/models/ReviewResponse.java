package com.project.models;

import java.util.List;

public class ReviewResponse {
    private List<ProductReview> data;
    private int total;

    public List<ProductReview> getData() {
        return data;
    }

    public void setData(List<ProductReview> data) {
        this.data = data;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
