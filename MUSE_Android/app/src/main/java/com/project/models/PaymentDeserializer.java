package com.project.models;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class PaymentDeserializer implements JsonDeserializer<Order.Payment> {
    @Override
    public Order.Payment deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        Order.Payment payment = new Order.Payment();
        if (json.isJsonPrimitive()) {
            // It's a string reference (e.g. ObjectId string)
            payment.set_id(json.getAsString());
        } else if (json.isJsonObject()) {
            JsonObject jsonObject = json.getAsJsonObject();
            if (jsonObject.has("_id")) {
                payment.set_id(jsonObject.get("_id").getAsString());
            }
            if (jsonObject.has("paymentMethod")) {
                payment.setPaymentMethod(jsonObject.get("paymentMethod").getAsString());
            }
            if (jsonObject.has("paymentStatus")) {
                payment.setPaymentStatus(jsonObject.get("paymentStatus").getAsString());
            }
            if (jsonObject.has("amount") && !jsonObject.get("amount").isJsonNull()) {
                payment.setAmount(jsonObject.get("amount").getAsDouble());
            }
            if (jsonObject.has("transactionId") && !jsonObject.get("transactionId").isJsonNull()) {
                payment.setTransactionId(jsonObject.get("transactionId").getAsString());
            }
            if (jsonObject.has("paymentDate") && !jsonObject.get("paymentDate").isJsonNull()) {
                payment.setPaymentDate(jsonObject.get("paymentDate").getAsString());
            }
        }
        return payment;
    }
}
