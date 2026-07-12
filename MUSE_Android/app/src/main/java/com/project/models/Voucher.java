package com.project.models;

import com.google.gson.annotations.SerializedName;

public class Voucher {
    @SerializedName("_id")
    private String id;
    private String code;
    private String promotionId;
    private transient Promotion promotion;
    private String status;
    private String orderId;
    private String usedDate;

    private String name;
    private String description;
    private double minOrderValue;
    private String expiryDate;
    private String type;
    private double discountValue;

    private boolean isSelected;

    public Voucher() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Promotion getPromotion() { return promotion; }
    public void setPromotion(Promotion promotion) { this.promotion = promotion; }

    public String getPromotionId() { 
        return promotionId != null ? promotionId : (promotion != null ? promotion.getId() : null); 
    }
    public void setPromotionId(String promotionId) { this.promotionId = promotionId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUsedDate() { return usedDate; }
    public void setUsedDate(String usedDate) { this.usedDate = usedDate; }

    public String getName() { 
        return promotion != null ? promotion.getName() : name; 
    }
    public void setName(String name) { this.name = name; }

    public String getDescription() { 
        return promotion != null ? promotion.getDescription() : description; 
    }
    public void setDescription(String description) { this.description = description; }

    public double getMinOrderValue() { 
        if (promotion != null && promotion.getConditions() != null && !promotion.getConditions().isEmpty()) {
            Double val = promotion.getConditions().get(0).getMinOrderValue();
            return val != null ? val : 0;
        }
        return minOrderValue; 
    }
    public void setMinOrderValue(double minOrderValue) { this.minOrderValue = minOrderValue; }

    public String getExpiryDate() { 
        return promotion != null ? promotion.getEndDate() : expiryDate; 
    }
    public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

    public String getType() { 
        if (promotion != null) {
            String pName = promotion.getName();
            if (pName != null) {
                pName = pName.toLowerCase();
                if (pName.contains("vận chuyển") || pName.contains("shipping")) {
                    return "SHIPPING";
                }
            }
        }
        return type != null ? type : "DISCOUNT"; 
    }
    public void setType(String type) { this.type = type; }

    public double getDiscountValue() {
        if (promotion != null && promotion.getConditions() != null && !promotion.getConditions().isEmpty()) {
            Double val = promotion.getConditions().get(0).getDiscountValue();
            return val != null ? val : 0;
        }
        return discountValue;
    }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public String getDiscountType() {
        if (promotion != null && promotion.getConditions() != null && !promotion.getConditions().isEmpty()) {
            return promotion.getConditions().get(0).getDiscountType();
        }
        return "vnd";
    }

    public double calculateDiscount(double orderTotal) {
        if (orderTotal < getMinOrderValue()) {
            return 0;
        }
        double discountVal = getDiscountValue();
        if ("percent".equalsIgnoreCase(getDiscountType())) {
            return orderTotal * (discountVal / 100.0);
        }
        return discountVal;
    }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}