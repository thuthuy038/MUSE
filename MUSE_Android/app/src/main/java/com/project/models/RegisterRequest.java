package com.project.models;

import java.util.Objects;

public class RegisterRequest {
    private String name;
    private String emailOrPhone;
    private String password;
    private String role = "customer";

    public RegisterRequest() {
    }

    public RegisterRequest(String name, String emailOrPhone, String password) {
        this.name = name;
        this.emailOrPhone = emailOrPhone;
        this.password = password;
    }

    public RegisterRequest(String name, String emailOrPhone, String password, String role) {
        this.name = name;
        this.emailOrPhone = emailOrPhone;
        this.password = password;
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmailOrPhone() {
        return emailOrPhone;
    }

    public void setEmailOrPhone(String emailOrPhone) {
        this.emailOrPhone = emailOrPhone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisterRequest that = (RegisterRequest) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(emailOrPhone, that.emailOrPhone) &&
                Objects.equals(password, that.password) &&
                Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, emailOrPhone, password, role);
    }

    @Override
    public String toString() {
        return "RegisterRequest{" +
                "name='" + name + '\'' +
                ", emailOrPhone='" + emailOrPhone + '\'' +
                ", password='" + password + '\'' +
                ", role='" + role + '\'' +
                '}';
    }
}
