package com.expert.project.noty.dto.ai.currentserver;

import com.expert.project.noty.entity.ChatConversation;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ChatConversationDTO {
    private Long id;
    private String question;
    private Boolean isUser;
    private LocalDateTime timestamp;

    // 생성자
    public ChatConversationDTO(ChatConversation entity) {
        this.id = entity.getId();
        this.question = entity.getMessage();
        this.isUser = entity.getIsUserMessage();
        this.timestamp = entity.getCreatedAt();
    }
}
