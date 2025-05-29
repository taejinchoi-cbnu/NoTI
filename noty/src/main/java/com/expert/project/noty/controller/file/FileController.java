package com.expert.project.noty.controller.file;


import com.expert.project.noty.dto.auth.CustomUserDetails;
import com.expert.project.noty.dto.file.AudioGetFileInformationResponse;
import com.expert.project.noty.dto.file.AudioNameModifyRespond;
import com.expert.project.noty.dto.file.AudioUploadRequest;
import com.expert.project.noty.entity.AudioFileEntity;
import com.expert.project.noty.service.ai.GeminiService;
import com.expert.project.noty.service.ai.WhisperService;
import com.expert.project.noty.service.file.AudioFileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @PostMapping("/get/file-information")
    public ResponseEntity<List<AudioGetFileInformationResponse>> getMyAudioFiles() {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        List<AudioGetFileInformationResponse> files = audioFileService.getAudioFilesByUserId(userId);

        return ResponseEntity.ok(files);
    }

    @PostMapping("/modify/name")
    public ResponseEntity<AudioNameModifyRespond> modifyFileName(
            @RequestParam("savedFileName") String savedFileName,
            @RequestParam("setName") String setName) {

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        String newSavedFileName = audioFileService.renameFile(userId, savedFileName, setName);

        AudioNameModifyRespond audioNameModifyRespond = new AudioNameModifyRespond(newSavedFileName);

        if (newSavedFileName.equals("fail")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(audioNameModifyRespond);
        } else {
            return ResponseEntity.ok(audioNameModifyRespond);
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<String> deleteFile(
            @RequestParam("savedFileName") String savedFileName) {

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        boolean deleted = audioFileService.deleteFile(userId, savedFileName);

        if (deleted) {
            return ResponseEntity.ok("파일 삭제 성공");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("파일 삭제 실패");
        }
    }

    @PostMapping("/download")
    public ResponseEntity<Resource> downloadAudioFile(
            @RequestParam("savedFileName") String savedFileName) {

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Resource resource = audioFileService.loadAudioFileAsResource(userId, savedFileName);

        if (resource == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String contentType;
        try {
            Path filePath = Paths.get(resource.getFile().getAbsolutePath());
            contentType = Files.probeContentType(filePath);
        } catch (IOException e) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION)
                .body(resource);
    }
}
