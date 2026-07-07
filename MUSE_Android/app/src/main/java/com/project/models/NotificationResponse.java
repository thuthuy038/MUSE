package com.project.models;

import java.io.Serializable;
import java.util.List;

public class NotificationResponse implements Serializable {
    private List<Notification> data;
    private int unreadCount;
    private int total;

    public List<Notification> getData() {
        return data;
    }

    public void setData(List<Notification> data) {
        this.data = data;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
