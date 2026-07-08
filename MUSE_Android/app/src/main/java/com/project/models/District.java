package com.project.models;

import java.util.List;

public class District {
    private String name;
    private String code;
    private String name_en;
    private List<Ward> wards;

    public District() {}

    public District(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName_en() { return name_en; }
    public void setName_en(String name_en) { this.name_en = name_en; }
    public List<Ward> getWards() { return wards; }
    public void setWards(List<Ward> wards) { this.wards = wards; }

    @Override
    public String toString() {
        return name;
    }
}
