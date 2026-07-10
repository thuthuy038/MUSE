package com.project.models;

import com.project.database.ShopMessage;
import java.util.List;

public class ChatResponse {
    private String customerId;
    private String customerName;
    private String avatar;
    private List<ShopMessage> messages;
    private String lastMessage;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public List<ShopMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ShopMessage> messages) {
        this.messages = messages;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}
