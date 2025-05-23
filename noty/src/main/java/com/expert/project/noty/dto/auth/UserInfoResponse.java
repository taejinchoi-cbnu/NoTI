package com.expert.project.noty.dto.auth;

import lombok.Getter;

@Getter
public class UserInfoResponse {
    private String userId;
    private String nickname;
    private String email;


    public UserInfoResponse(String userId, String nickname, String email) {
        this.userId = userId;
        this.nickname = nickname;
        this.email = email;
    }
}
