package com.project.models;

public class Order {
    private String id;
    private double totalAmount;
    private String status;
    private String createdAt;

    public Order() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
