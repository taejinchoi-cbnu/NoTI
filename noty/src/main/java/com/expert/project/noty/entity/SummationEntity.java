package com.expert.project.noty.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
public class SummationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String summation;

    @Lob // 대용량 객체로 정의
    @Column(columnDefinition = "MEDIUMTEXT") // MySQL에서 MEDIUMTEXT로 정의
    private String stt;

    private String savedFileName;

    private String userId;
    private LocalDateTime uploadDate;

    private Long audioId;
}
