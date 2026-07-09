package com.project.models;

import java.util.List;

public class Province {
    private String name;
    private String code;
    private String name_en;
    private String codename;
    private List<District> districts;
    private List<Ward> wards;

    public Province() {}

    public Province(String name, String code, String name_en) {
        this.name = name;
        this.code = code;
        this.name_en = name_en;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName_en() { return name_en; }
    public void setName_en(String name_en) { this.name_en = name_en; }
    public String getCodename() { return codename; }
    public void setCodename(String codename) { this.codename = codename; }
    public List<District> getDistricts() { return districts; }
    public void setDistricts(List<District> districts) { this.districts = districts; }
    public List<Ward> getWards() { return wards; }
    public void setWards(List<Ward> wards) { this.wards = wards; }

    @Override
    public String toString() {
        return name;
    }
}
