package com.expert.project.noty.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class ChatbotSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userId;

    @ManyToOne
    @JoinColumn(name = "audio_file_id")
    private AudioFileEntity audioFile;

    @Enumerated(EnumType.STRING)
    private SessionStatus sessionStatus;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String fullContext;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
