package com.expert.project.noty.service.ai;

import com.expert.project.noty.entity.AudioFileEntity;
import com.expert.project.noty.entity.SummationEntity;
import com.expert.project.noty.entity.UserEntity;
import com.expert.project.noty.repository.AudioFileRepository;
import com.expert.project.noty.repository.SummationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;

@Service
public class AudioProcessingService {

    private final WhisperService whisperService;
    private final SummationRepository summationRepository;
    private final AudioFileRepository audioFileRepository;

    public AudioProcessingService(WhisperService whisperService,
                                  SummationRepository summationRepository,
                                  AudioFileRepository audioFileRepository) {

        this.whisperService = whisperService;
        this.summationRepository = summationRepository;
        this.audioFileRepository = audioFileRepository;
    }

    @Async
    public void processAudioAsync(File audioFile, String username) {
        try {
            // AI 처리 로직 (예: Whisper API 호출)
            String stt = whisperService.transcribe(audioFile).join();

            Optional<AudioFileEntity> audioFileEntity = audioFileRepository.findBySavedName(audioFile.getName());

            // 결과 저장 또는 후속 작업
            System.out.println("요약 결과: " + stt);
            // DB 저장
            if (stt != null) {
                SummationEntity summation = new SummationEntity();
                summation.setSavedFileName(audioFile.getName());

                audioFileEntity.ifPresent(fileEntity -> summation.setAudioId(fileEntity.getId()));

                summation.setUploadDate(LocalDateTime.now());
                summation.setUserId(username);
                summation.setStt(stt);
                summationRepository.save(summation);
            }

        } catch (Exception e) {
            System.out.println("AI 처리 실패: " + e);
        }
    }
}
