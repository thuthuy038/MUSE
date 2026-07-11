package com.project.models;

public class SavedOutfit {
    private String setName;
    private String topName;
    private double topPrice;
    private String topImageUrl;
    private String topId;
    
    private String bottomName;
    private double bottomPrice;
    private String bottomImageUrl;
    private String bottomId;
    
    private String savedDate;

    public SavedOutfit(String setName, String topName, double topPrice, String topImageUrl, String topId,
                       String bottomName, double bottomPrice, String bottomImageUrl, String bottomId, String savedDate) {
        this.setName = setName;
        this.topName = topName;
        this.topPrice = topPrice;
        this.topImageUrl = topImageUrl;
        this.topId = topId;
        this.bottomName = bottomName;
        this.bottomPrice = bottomPrice;
        this.bottomImageUrl = bottomImageUrl;
        this.bottomId = bottomId;
        this.savedDate = savedDate;
    }

    public String getSetName() { return setName; }
    public String getTopName() { return topName; }
    public double getTopPrice() { return topPrice; }
    public String getTopImageUrl() { return topImageUrl; }
    public String getTopId() { return topId; }
    public String getBottomName() { return bottomName; }
    public double getBottomPrice() { return bottomPrice; }
    public String getBottomImageUrl() { return bottomImageUrl; }
    public String getBottomId() { return bottomId; }
    public String getSavedDate() { return savedDate; }
}
