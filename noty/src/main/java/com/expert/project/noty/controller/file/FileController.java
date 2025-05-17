package com.expert.project.noty.controller.file;


import com.expert.project.noty.dto.auth.CustomUserDetails;
import com.expert.project.noty.dto.file.AudioGetFileInformationResponse;
import com.expert.project.noty.dto.file.AudioUploadRequest;
import com.expert.project.noty.entity.AudioFileEntity;
import com.expert.project.noty.service.ai.GeminiService;
import com.expert.project.noty.service.ai.WhisperService;
import com.expert.project.noty.service.file.AudioFileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/file")
public class FileController {

    private final AudioFileService audioFileService;

    public FileController(AudioFileService audioFileService) {
        this.audioFileService = audioFileService;
    }

    @PostMapping("/upload/audio")
    public ResponseEntity<String> uploadAudio(@ModelAttribute AudioUploadRequest request) {

        try {
            String fileName = audioFileService.saveAudio(request);

            return ResponseEntity.ok("파일 저장 완료: " + fileName);
        } catch(IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("파일 저장 실패: " + e.getMessage());
        }
    }

    // TODO: 현재 저장되어 있는 유저의 파일 데이터 전송
    @PostMapping("/get/file-information")
    public ResponseEntity<List<AudioGetFileInformationResponse>> getMyAudioFiles(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // JWT로부터 userId 추출 (예시로 username이라고 가정)
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        List<AudioGetFileInformationResponse> files = audioFileService.getAudioFilesByUserId(userId);

        return ResponseEntity.ok(files);
    }
}
