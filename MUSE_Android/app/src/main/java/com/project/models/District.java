package com.project.models;

import java.util.List;

public class District {
    private String name;
    private int code;
    private List<Ward> wards;

    public District() {}

    public District(String name, int code) {
        this.name = name;
        this.code = code;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public List<Ward> getWards() { return wards; }
    public void setWards(List<Ward> wards) { this.wards = wards; }

    @Override
    public String toString() {
        return name;
    }
}
