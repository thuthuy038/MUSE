package com.project.models;

import java.util.List;

public class Province {
    private String name;
    private int code;
    private String codename;
    private List<District> districts;

    public Province() {}

    public Province(String name, int code, String codename) {
        this.name = name;
        this.code = code;
        this.codename = codename;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getCodename() { return codename; }
    public void setCodename(String codename) { this.codename = codename; }
    public List<District> getDistricts() { return districts; }
    public void setDistricts(List<District> districts) { this.districts = districts; }

    @Override
    public String toString() {
        return name;
    }
}
