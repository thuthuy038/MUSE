package com.project.models;

public class Ward {
    private String name;
    private int code;

    public Ward() {}

    public Ward(String name, int code) {
        this.name = name;
        this.code = code;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    @Override
    public String toString() {
        return name;
    }
}
