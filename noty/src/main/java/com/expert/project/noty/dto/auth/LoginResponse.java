package com.expert.project.noty.dto.auth;

public class LoginResponse {
    private String message;

    public LoginResponse(String message) {
        this.message = message;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
