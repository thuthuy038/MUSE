package com.project.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Promotion {
    @SerializedName("_id")
    private String id;
    private String code;
    private String name;
    private String description;
    private String startDate;
    private String endDate;
    private String status;
    private List<Condition> conditions;

    public Promotion() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<Condition> getConditions() { return conditions; }
    public void setConditions(List<Condition> conditions) { this.conditions = conditions; }

    public static class Condition {
        private double minOrderValue;

        public double getMinOrderValue() { return minOrderValue; }
        public void setMinOrderValue(double minOrderValue) { this.minOrderValue = minOrderValue; }
    }
}
