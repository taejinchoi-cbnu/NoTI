package com.expert.project.noty.repository;

import com.expert.project.noty.entity.AudioFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AudioFileRepository extends JpaRepository<AudioFileEntity, Long> {

    Optional<AudioFileEntity> findBySavedName(String savedName);
    List<AudioFileEntity> findByUserId(String userId);
}
