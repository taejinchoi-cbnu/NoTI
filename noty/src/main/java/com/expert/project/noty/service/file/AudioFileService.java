package com.expert.project.noty.service.file;

import com.expert.project.noty.dto.file.AudioUploadRequest;
import com.expert.project.noty.entity.AudioFileEntity;
import com.expert.project.noty.repository.AudioFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AudioFileService {

    @Value("${file.upload.dir}")
    private String baseDir;

    private final AudioFileRepository audioFileRepository;

    public AudioFileService(AudioFileRepository audioFileRepository) {
        this.audioFileRepository = audioFileRepository;
    }

    public String saveAudio(AudioUploadRequest request) throws IOException {
        MultipartFile file = request.getFile();

        String uploadDir = baseDir + "/uploads/audio/" + LocalDate.now();
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String savedName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(savedName);
        System.out.println(filePath);

        System.out.println("파일 이름: " + file.getOriginalFilename());
        System.out.println("파일 크기: " + file.getSize());

//        file.transferTo(filePath.toFile());

        try {
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            System.out.println("파일 저장 중 오류 발생:");
            e.printStackTrace();
            throw new RuntimeException("파일 저장 실패", e);
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        AudioFileEntity audioFileEntity = new AudioFileEntity();
        audioFileEntity.setOriginalName(file.getOriginalFilename());
        audioFileEntity.setSavedName(savedName);
        audioFileEntity.setFilePath(filePath.toString());
        audioFileEntity.setFileSize(file.getSize());
        audioFileEntity.setFileType(file.getContentType());
        audioFileEntity.setUploadDate(LocalDateTime.now());
        audioFileEntity.setUserId(username);
        audioFileEntity.setDuration(request.getDuration());

        audioFileRepository.save(audioFileEntity);

        return savedName;
    }
}
