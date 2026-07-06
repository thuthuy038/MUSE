package com.project.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class ProductVariant implements Serializable, Parcelable {

    private String size;
    private String color;
    private int quantity;

    protected ProductVariant(Parcel in) {
        size = in.readString();
        color = in.readString();
        quantity = in.readInt();
    }

    public static final Creator<ProductVariant> CREATOR = new Creator<ProductVariant>() {
        @Override
        public ProductVariant createFromParcel(Parcel in) {
            return new ProductVariant(in);
        }

        @Override
        public ProductVariant[] newArray(int size) {
            return new ProductVariant[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(size);
        dest.writeString(color);
        dest.writeInt(quantity);
    }

    public ProductVariant() {}

    // getter setter

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}