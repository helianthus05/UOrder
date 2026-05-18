package com.example.uorder.core.models;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "food_items")
public class FoodItem implements Parcelable {

    @PrimaryKey
    @NonNull
    private String id = "";
    private String name;
    private String description;
    private double price;
    private int image;
    private String imageUri; // URI string for custom uploaded images
    private String stallId;

    @Ignore
    private boolean selected = false;
    @Ignore
    private int quantity = 1;

    public FoodItem() {
        // Default constructor for Firebase
    }

    @Ignore
    public FoodItem(String name, double price, String description) {
        this.name = name;
        this.price = price;
        this.description = description;
    }

    @Ignore
    public FoodItem(String name, String description, double price, int image) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.image = image;
    }

    @Ignore
    public FoodItem(String name, String description, double price, String imageUri) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUri = imageUri;
    }

    protected FoodItem(Parcel in) {
        id = in.readString();
        name = in.readString();
        description = in.readString();
        price = in.readDouble();
        image = in.readInt();
        imageUri = in.readString();
        selected = in.readByte() != 0;
        quantity = in.readInt();
        stallId = in.readString();
    }

    public static final Creator<FoodItem> CREATOR = new Creator<FoodItem>() {
        @Override
        public FoodItem createFromParcel(Parcel in) {
            return new FoodItem(in);
        }

        @Override
        public FoodItem[] newArray(int size) {
            return new FoodItem[size];
        }
    };

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
    public int getImage() { return image; }
    public String getImageUri() { return imageUri; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(double price) { this.price = price; }
    public void setImage(int image) { this.image = image; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public String getStallId() { return stallId; }
    public void setStallId(String stallId) { this.stallId = stallId; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeDouble(price);
        dest.writeInt(image);
        dest.writeString(imageUri);
        dest.writeByte((byte) (selected ? 1 : 0));
        dest.writeInt(quantity);
        dest.writeString(stallId);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
