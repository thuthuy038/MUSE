package com.project.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ColorListDeserializer implements JsonDeserializer<List<String>> {
    @Override
    public List<String> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<String> colors = new ArrayList<>();
        if (json.isJsonPrimitive()) {
            // It's a single string, e.g., "Trắng"
            colors.add(json.getAsString());
        } else if (json.isJsonArray()) {
            // It's an array, e.g., ["Trắng", "Đen"]
            JsonArray array = json.getAsJsonArray();
            for (JsonElement element : array) {
                if (element.isJsonPrimitive()) {
                    colors.add(element.getAsString());
                }
            }
        }
        return colors;
    }
}
