package com.project.models;

import java.util.List;
import java.util.Objects;

public class User {
    private String _id;
    private String code;
    private String name;
    private String email;
    private String phone = null;
    private String role;

    private Avatar avatar = null;

    private List<Address> addresses = null;
    private Payment payment = null;
    private int points = 0;
    private int level = 1;
    private String createdAt = null;
    private int orderCount = 0;

    public static class Address {
        private String street;
        private String ward;
        private String district;
        private String province;
        private String addressNote;
        private boolean isDefault;

        public Address() {}

        public Address(String street, String ward, String district, String province, String addressNote, boolean isDefault) {
            this.street = street;
            this.ward = ward;
            this.district = district;
            this.province = province;
            this.addressNote = addressNote;
            this.isDefault = isDefault;
        }

        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }

        public String getWard() { return ward; }
        public void setWard(String ward) { this.ward = ward; }

        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }

        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }

        public String getAddressNote() { return addressNote; }
        public void setAddressNote(String addressNote) { this.addressNote = addressNote; }

        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    }

    public static class Payment {
        private String accountNumber;
        private String accountName;
        private String bank;

        public Payment() {}

        public Payment(String accountNumber, String accountName, String bank) {
            this.accountNumber = accountNumber;
            this.accountName = accountName;
            this.bank = bank;
        }

        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

        public String getAccountName() { return accountName; }
        public void setAccountName(String accountName) { this.accountName = accountName; }

        public String getBank() { return bank; }
        public void setBank(String bank) { this.bank = bank; }
    }

    public static class Avatar {
        private String gridfsFileId;
        private String filename;
        private String originalname;
        private String mimetype;
        private int size;
        private String url;

        public Avatar() {}

        public Avatar(String url) {
            this.url = url;
        }

        public String getGridfsFileId() { return gridfsFileId; }
        public void setGridfsFileId(String gridfsFileId) { this.gridfsFileId = gridfsFileId; }

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }

        public String getOriginalname() { return originalname; }
        public void setOriginalname(String originalname) { this.originalname = originalname; }

        public String getMimetype() { return mimetype; }
        public void setMimetype(String mimetype) { this.mimetype = mimetype; }

        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    public User() {
    }

    public User(String _id, String code, String name, String email, String phone, String role, Avatar avatar, List<Address> addresses, Payment payment) {
        this._id = _id;
        this.code = code;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.avatar = avatar;
        this.addresses = addresses;
        this.payment = payment;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Avatar getAvatar() {
        return avatar;
    }

    public void setAvatar(Avatar avatar) {
        this.avatar = avatar;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(_id, user._id) &&
                Objects.equals(code, user.code) &&
                Objects.equals(name, user.name) &&
                Objects.equals(email, user.email) &&
                Objects.equals(phone, user.phone) &&
                Objects.equals(role, user.role) &&
                Objects.equals(avatar, user.avatar) &&
                Objects.equals(addresses, user.addresses) &&
                Objects.equals(payment, user.payment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(_id, code, name, email, phone, role, avatar, addresses, payment);
    }

    @Override
    public String toString() {
        return "User{" +
                "_id='" + _id + '\'' +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", role='" + role + '\'' +
                ", avatar=" + avatar +
                ", addresses=" + addresses +
                ", payment=" + payment +
                '}';
    }
}
