package com.expert.project.noty.dto.ai.currentserver;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageRequest {
    private Long sessionId;
    private String message;
}
