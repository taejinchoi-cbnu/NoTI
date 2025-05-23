package com.expert.project.noty.service.file;

import com.expert.project.noty.dto.file.AudioGetFileInformationResponse;
import com.expert.project.noty.dto.file.AudioUploadRequest;
import com.expert.project.noty.entity.AudioFileEntity;
import com.expert.project.noty.repository.AudioFileRepository;
import com.expert.project.noty.service.ai.AudioProcessingService;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class    AudioFileService {

    @Value("${file.upload.dir}")
    private String baseDir;

    private final AudioFileRepository audioFileRepository;
    private final AudioProcessingService audioProcessingService;


    public AudioFileService(AudioFileRepository audioFileRepository, AudioProcessingService audioProcessingService) {
        this.audioFileRepository = audioFileRepository;
        this.audioProcessingService = audioProcessingService;
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

        try {
            file.transferTo(filePath.toFile());
        } catch (IOException e) {
            System.out.println("파일 저장 중 오류 발생:");
            e.printStackTrace();
            throw new RuntimeException("파일 저장 실패", e);
        }

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        // AI 비동기 처리
        audioProcessingService.processAudioAsync(filePath.toFile(), username);
        System.out.println("File path : " + filePath.toFile().getName());

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

    public List<AudioGetFileInformationResponse> getAudioFilesByUserId(String userId) {
        return audioFileRepository.findByUserId(userId).stream()
                .map(file -> AudioGetFileInformationResponse.builder()
                        .originalName(file.getOriginalName())
                        .savedName(file.getSavedName())
                        .filePath(file.getFilePath())
                        .fileSize(file.getFileSize())
                        .fileType(file.getFileType())
                        .uploadDate(file.getUploadDate())
                        .duration(file.getDuration())
                        .build()
                ).collect(Collectors.toList());
    }

    public String renameFile(String userId, String savedFileName, String setName) {
        Optional<AudioFileEntity> optionalFile = audioFileRepository.findByUserIdAndSavedName(userId, savedFileName);
        if (optionalFile.isPresent()) {
            AudioFileEntity fileEntity = optionalFile.get();

            String savedName = UUID.randomUUID() + "_" + setName;
            fileEntity.setOriginalName(setName);
            fileEntity.setSavedName(savedName);

            audioFileRepository.save(fileEntity);

            return savedName;
        }

        return "fail";
    }
}
