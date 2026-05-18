package com.example.uorder.core.models;

import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Order {
    private String orderId;
    private String customerId;
    private String customerName;
    private String stallId;
    private String stallName;
    private Map<String, Integer> orderItems; // productName -> quantity
    private double totalAmount;
    private String orderStatus; // Pending, Accepted, Rejected, Completed
    private boolean adminDismissed = false;
    private String dayString;
    private String monthString;
    private String hourString;
    private Timestamp timestamp;
    private String parentOrderId; // Link split orders to a common parent

    // Static cart state used by CheckoutActivity (Transient)
    private static List<Product> globalCart = new ArrayList<>();

    public Order() {
        // Default constructor for Firebase
    }

    public Order(String customerId, String customerName, String stallId, Map<String, Integer> orderItems, double totalAmount) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.stallId = stallId;
        this.orderItems = orderItems;
        this.totalAmount = totalAmount;
        this.orderStatus = "Pending";
        this.timestamp = Timestamp.now();

        java.util.Calendar cal = java.util.Calendar.getInstance();
        this.dayString = String.valueOf(cal.get(java.util.Calendar.DAY_OF_MONTH));
        this.monthString = String.valueOf(cal.get(java.util.Calendar.MONTH) + 1);
        this.hourString = String.valueOf(cal.get(java.util.Calendar.HOUR_OF_DAY));
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getStallId() { return stallId; }
    public void setStallId(String stallId) { this.stallId = stallId; }

    public String getStallName() { return stallName; }
    public void setStallName(String stallName) { this.stallName = stallName; }

    public Map<String, Integer> getOrderItems() { return orderItems; }
    public void setOrderItems(Map<String, Integer> orderItems) { this.orderItems = orderItems; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public boolean isAdminDismissed() { return adminDismissed; }
    public void setAdminDismissed(boolean adminDismissed) { this.adminDismissed = adminDismissed; }

    public String getDayString() { return dayString; }
    public void setDayString(String dayString) { this.dayString = dayString; }

    public String getMonthString() { return monthString; }
    public void setMonthString(String monthString) { this.monthString = monthString; }

    public String getHourString() { return hourString; }
    public void setHourString(String hourString) { this.hourString = hourString; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getParentOrderId() { return parentOrderId; }
    public void setParentOrderId(String parentOrderId) { this.parentOrderId = parentOrderId; }

    // Cart management for Student UI
    public static void setCart(List<Product> items) {
        globalCart = new ArrayList<>(items);
    }

    public static void clearCart() {
        for (Product product : globalCart) {
            product.setSelected(false);
            product.setQuantity(1);
        }
        globalCart.clear();
    }

    public static List<Product> getCart() {
        return globalCart;
    }
}
