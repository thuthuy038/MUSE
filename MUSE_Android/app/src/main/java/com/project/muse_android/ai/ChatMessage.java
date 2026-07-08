package com.project.muse_android.ai;

import com.project.models.Product;
import java.util.List;

public class ChatMessage {
    private String text;
    private boolean isUser;
    private List<Product> suggestedProducts;

    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
        this.suggestedProducts = null;
    }

    public ChatMessage(String text, boolean isUser, List<Product> suggestedProducts) {
        this.text = text;
        this.isUser = isUser;
        this.suggestedProducts = suggestedProducts;
    }

    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }

    public List<Product> getSuggestedProducts() {
        return suggestedProducts;
    }
}
