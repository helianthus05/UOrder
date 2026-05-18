package com.example.uorder.core.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Product implements Parcelable {
    private String id; // This will store the Auto-ID from Firestore
    private String productName;
    private double productPrice;
    private String productDescription;
    private String productImageUrl;
    private String productType; // Meal, Snack, Beverage, Dessert, Side Dish
    private String stallId;
    private boolean isAvailable;

    // Transient fields for UI/Cart state
    private boolean selected = false;
    private int quantity = 1;

    public Product() {
        // Default constructor for Firebase
    }

    public Product(String productName, double productPrice, String productDescription, String productType, String stallId, boolean isAvailable, String productImageUrl) {
        this.productName = productName;
        this.productPrice = productPrice;
        this.productDescription = productDescription;
        this.productType = productType;
        this.stallId = stallId;
        this.isAvailable = isAvailable;
        this.productImageUrl = productImageUrl;
    }

    protected Product(Parcel in) {
        id = in.readString();
        productName = in.readString();
        productPrice = in.readDouble();
        productDescription = in.readString();
        productImageUrl = in.readString();
        productType = in.readString();
        stallId = in.readString();
        isAvailable = in.readByte() != 0;
        selected = in.readByte() != 0;
        quantity = in.readInt();
    }

    public static final Creator<Product> CREATOR = new Creator<Product>() {
        @Override
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        @Override
        public Product[] newArray(int size) {
            return new Product[size];
        }
    };

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public double getProductPrice() { return productPrice; }
    public void setProductPrice(double productPrice) { this.productPrice = productPrice; }

    public String getProductDescription() { return productDescription; }
    public void setProductDescription(String productDescription) { this.productDescription = productDescription; }

    public String getProductImageUrl() { return productImageUrl; }
    public void setProductImageUrl(String productImageUrl) { this.productImageUrl = productImageUrl; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getStallId() { return stallId; }
    public void setStallId(String stallId) { this.stallId = stallId; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    @com.google.firebase.firestore.Exclude
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    @com.google.firebase.firestore.Exclude
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(productName);
        dest.writeDouble(productPrice);
        dest.writeString(productDescription);
        dest.writeString(productImageUrl);
        dest.writeString(productType);
        dest.writeString(stallId);
        dest.writeByte((byte) (isAvailable ? 1 : 0));
        dest.writeByte((byte) (selected ? 1 : 0));
        dest.writeInt(quantity);
    }
}
