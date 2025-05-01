package com.expert.project.noty.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class AudioFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String originalName;
    private String savedName;
    private String filePath;
    private Long fileSize;
    private String fileType;

    private LocalDateTime uploadDate;
    private String userId;

    private Integer duration; // 녹음 길이
}
