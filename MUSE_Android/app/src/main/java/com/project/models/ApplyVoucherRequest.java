package com.project.models;

public class ApplyVoucherRequest {
    private String code;
    private String orderId;

    public ApplyVoucherRequest(String code, String orderId) {
        this.code = code;
        this.orderId = orderId;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}
