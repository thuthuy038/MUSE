package com.project.models;

import java.util.List;

public class ArchivedChat {
    private String id;
    private String title;
    private String date;
    private List<ChatMessage> messages;

    public ArchivedChat(String id, String title, String date, List<ChatMessage> messages) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.messages = messages;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public List<ChatMessage> getMessages() { return messages; }
}
