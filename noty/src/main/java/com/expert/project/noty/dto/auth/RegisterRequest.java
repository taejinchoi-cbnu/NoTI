package com.expert.project.noty.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private Long id;
    private String userId;
    private String password;
    private String nickname;
    private String email;
}
