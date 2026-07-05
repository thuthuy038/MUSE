package com.project.models;

import com.google.gson.annotations.SerializedName;

public class Voucher {
    @SerializedName("_id")
    private String id;
    private String code;
    private String promotionId;
    private String status;
    private String orderId;
    private String usedDate;

    // Display info (mapped from Promotion)
    private String name;
    private String description;
    private double minOrderValue;
    private String expiryDate;
    private String type;

    // Local UI state
    private boolean isSelected;

    public Voucher() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getPromotionId() { return promotionId; }
    public void setPromotionId(String promotionId) { this.promotionId = promotionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUsedDate() { return usedDate; }
    public void setUsedDate(String usedDate) { this.usedDate = usedDate; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getMinOrderValue() { return minOrderValue; }
    public void setMinOrderValue(double minOrderValue) { this.minOrderValue = minOrderValue; }

    public String getExpiryDate() { return expiryDate; }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}
