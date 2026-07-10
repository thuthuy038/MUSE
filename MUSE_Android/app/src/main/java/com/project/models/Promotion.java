package com.project.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;

public class Promotion implements Serializable {
    @SerializedName("_id")
    private String id;
    private String code;
    private String name;
    private String description;
    private String startDate;
    private String endDate;
    private String status;
    private String promotionType;
    private String promotionMethod;
    private List<Condition> conditions;
    private VoucherInfo voucher;

    public Promotion() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public VoucherInfo getVoucher() { return voucher; }
    public void setVoucher(VoucherInfo voucher) { this.voucher = voucher; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPromotionType() { return promotionType; }
    public void setPromotionType(String promotionType) { this.promotionType = promotionType; }

    public String getPromotionMethod() { return promotionMethod; }
    public void setPromotionMethod(String promotionMethod) { this.promotionMethod = promotionMethod; }

    public List<Condition> getConditions() { return conditions; }
    public void setConditions(List<Condition> conditions) { this.conditions = conditions; }

    public static class Condition implements Serializable {
        private Double minOrderValue;
        private String buyProductId;
        private Integer buyQuantity;
        private Double discountValue;
        private String discountType; // 'percent', 'vnd'
        private String giftProductId;
        private Integer giftQuantity;

        public Double getMinOrderValue() { return minOrderValue; }
        public void setMinOrderValue(Double minOrderValue) { this.minOrderValue = minOrderValue; }

        public String getBuyProductId() { return buyProductId; }
        public void setBuyProductId(String buyProductId) { this.buyProductId = buyProductId; }

        public Integer getBuyQuantity() { return buyQuantity; }
        public void setBuyQuantity(Integer buyQuantity) { this.buyQuantity = buyQuantity; }

        public Double getDiscountValue() { return discountValue; }
        public void setDiscountValue(Double discountValue) { this.discountValue = discountValue; }

        public String getDiscountType() { return discountType; }
        public void setDiscountType(String discountType) { this.discountType = discountType; }

        public String getGiftProductId() { return giftProductId; }
        public void setGiftProductId(String giftProductId) { this.giftProductId = giftProductId; }

        public Integer getGiftQuantity() { return giftQuantity; }
        public void setGiftQuantity(Integer giftQuantity) { this.giftQuantity = giftQuantity; }
    }

    public static class VoucherInfo implements java.io.Serializable {
        private int quantity;
        private String prefix;
        private String suffix;

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }
        public String getSuffix() { return suffix; }
        public void setSuffix(String suffix) { this.suffix = suffix; }
    }
}