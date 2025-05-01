package com.expert.project.noty.dto.auth;

public class ModifyResponse {
    private String message;

    public ModifyResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
