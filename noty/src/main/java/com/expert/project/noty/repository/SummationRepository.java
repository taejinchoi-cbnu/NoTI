package com.expert.project.noty.repository;

import com.expert.project.noty.entity.SummationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SummationRepository extends JpaRepository<SummationEntity, Long> {
//    Optional<SummationEntity> findBySavedFileNameAndUserIdAndAudioId(String savedFileName, String userId, Long audioId);
    Optional<SummationEntity> findByUserIdAndAudioId(String userId, Long audioId);
}
