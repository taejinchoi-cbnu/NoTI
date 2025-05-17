package com.expert.project.noty.service.ai;

import com.expert.project.noty.dto.ai.currentserver.STTResponse;
import com.expert.project.noty.entity.AudioFileEntity;
import com.expert.project.noty.entity.SummationEntity;
import com.expert.project.noty.exception.ResourceNotFoundException;
import com.expert.project.noty.repository.AudioFileRepository;
import com.expert.project.noty.repository.SummationRepository;
import org.springframework.stereotype.Service;

@Service
public class STTService {

    private final AudioFileRepository audioFileRepository;
    private final SummationRepository summationRepository;

    public STTService(AudioFileRepository audioFileRepository,
                      SummationRepository summationRepository) {
        this.audioFileRepository = audioFileRepository;
        this.summationRepository = summationRepository;
    }

    public STTResponse getSummationBySavedFileName(String savedFileName, String userId) {

        // savedFileName으로 audio 파일 찾기
        AudioFileEntity audioFileEntity = audioFileRepository.findBySavedName(savedFileName)
                .orElseThrow(() -> new ResourceNotFoundException("Audio file not found"));

        // 해당 사용자와 오디오 ID에 맞는 Summation 조회
        SummationEntity summationEntity = summationRepository.findBySavedFileNameAndUserIdAndAudioId(
                        savedFileName, userId, audioFileEntity.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Summation not found"));

//        System.out.println("summation: " + summationEntity.getSummation());

        return new STTResponse(summationEntity.getSummation());
    }
}
