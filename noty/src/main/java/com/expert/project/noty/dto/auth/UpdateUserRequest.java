package com.expert.project.noty.dto.auth;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    private String userId;
    private String nickname;
    private String email;
}
