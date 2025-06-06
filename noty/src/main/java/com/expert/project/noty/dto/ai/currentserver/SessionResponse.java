package com.expert.project.noty.dto.ai.currentserver;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class SessionResponse {
    private Long sessionId;
    private String sessionStatus;
}
