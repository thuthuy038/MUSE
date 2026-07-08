package com.project.models;

import java.io.Serializable;
import java.util.List;

public class Order implements Serializable {
    private String id;
    private String status; // PENDING, PROCESSING, SHIPPING, DELIVERED, RETURNED, CANCELLED
    private List<Product> products;
    private double totalPrice;
    private String orderDate;

    public Order() {}

    public Order(String id, String status, List<Product> products, double totalPrice) {
        this.id = id;
        this.status = status;
        this.products = products;
        this.totalPrice = totalPrice;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getOrderDate() { return orderDate; }
    public void setOrderDate(String orderDate) { this.orderDate = orderDate; }


}
