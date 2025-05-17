package com.expert.project.noty.dto.file;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AudioGetFileInformationResponse {
    private String originalName;
    private String savedName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private LocalDateTime uploadDate;
    private Integer duration;
}
