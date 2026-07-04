package com.project.models;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class ProductImageDeserializer implements JsonDeserializer<Product.ProductImage> {
    @Override
    public Product.ProductImage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Product.ProductImage productImage = new Product.ProductImage();
        if (json.isJsonPrimitive()) {
            // It's a string, e.g., "url/to/image.jpg"
            productImage.setUrl(json.getAsString());
        } else if (json.isJsonObject()) {
            // It's an object, e.g., {"url": "url/to/image.jpg"}
            JsonObject jsonObject = json.getAsJsonObject();
            if (jsonObject.has("url")) {
                productImage.setUrl(jsonObject.get("url").getAsString());
            }
        }
        return productImage;
    }
}
