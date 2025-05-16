package com.expert.project.noty.dto.file;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
public class AudioUploadRequest {

    private MultipartFile file;

    private Integer duration;
}
