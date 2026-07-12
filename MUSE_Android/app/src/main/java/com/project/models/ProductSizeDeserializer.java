package com.project.models;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class ProductSizeDeserializer implements JsonDeserializer<Product.ProductSize> {
    @Override
    public Product.ProductSize deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Product.ProductSize productSize = new Product.ProductSize();
        if (json.isJsonPrimitive()) {
            productSize.setSize(json.getAsString());
            productSize.setQuantity(0); // Default quantity
        } else if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            if (jsonObject.has("size")) {
                productSize.setSize(jsonObject.get("size").getAsString());
            }
            if (jsonObject.has("quantity")) {
                productSize.setQuantity(jsonObject.get("quantity").getAsInt());
            }
        }
        return productSize;
    }
}
