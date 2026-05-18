package com.example.uorder.core.models;

import java.util.ArrayList;
import java.util.List;

public class Stall {
    private String stallName;
    private String stallEmail;
    private String stallPhone;
    private String stallLogoUrl;
    private String stallBg;
    private String stallDescription;
    private String facebookLink;
    private String ownerId;
    private List<Product> items; // Local helper, not stored as a field in stalls collection

    public Stall() {
        this.items = new ArrayList<>();
    }

    public Stall(String stallName, String stallEmail, String stallPhone, String stallLogoUrl, String ownerId) {
        this.stallName = stallName;
        this.stallEmail = stallEmail;
        this.stallPhone = stallPhone;
        this.stallLogoUrl = stallLogoUrl;
        this.ownerId = ownerId;
        this.stallDescription = "";
        this.facebookLink = "";
        this.items = new ArrayList<>();
    }

    public String getStallName() { return stallName; }
    public void setStallName(String stallName) { this.stallName = stallName; }

    public String getStallEmail() { return stallEmail; }
    public void setStallEmail(String stallEmail) { this.stallEmail = stallEmail; }

    public String getStallPhone() { return stallPhone; }
    public void setStallPhone(String stallPhone) { this.stallPhone = stallPhone; }

    public String getStallLogoUrl() { return stallLogoUrl; }
    public void setStallLogoUrl(String stallLogoUrl) { this.stallLogoUrl = stallLogoUrl; }

    public String getStallBg() { return stallBg; }
    public void setStallBg(String stallBg) { this.stallBg = stallBg; }

    public String getStallDescription() { return stallDescription; }
    public void setStallDescription(String stallDescription) { this.stallDescription = stallDescription; }

    public String getFacebookLink() { return facebookLink; }
    public void setFacebookLink(String facebookLink) { this.facebookLink = facebookLink; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public List<Product> getItems() { return items; }
    public void setItems(List<Product> items) { this.items = items; }
}