package com.project.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shop_messages")
public class ShopMessage {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String sender; // "customer", "admin", "guest"
    private String content; 
    private String image; // Base64 string for image support
    private String timestamp; // Changed from long to String to support ISO date strings from server
    private String userId; // maps to customerId

    public ShopMessage(String sender, String content, String image, String timestamp, String userId) {
        this.sender = sender;
        this.content = content;
        this.image = image;
        this.timestamp = timestamp;
        this.userId = userId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
