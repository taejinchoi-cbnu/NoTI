package com.expert.project.noty.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class ChatConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private ChatbotSession session;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String message;

    private Boolean isUserMessage;
    private LocalDateTime createdAt;
}