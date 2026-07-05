package com.project.models;

public class ApplyVoucherResponse {
    private String message;
    private Voucher voucher;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Voucher getVoucher() { return voucher; }
    public void setVoucher(Voucher voucher) { this.voucher = voucher; }
}
