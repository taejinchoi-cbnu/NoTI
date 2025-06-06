package com.expert.project.noty.repository;

import com.expert.project.noty.entity.ChatbotSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.bind.support.SessionStatus;

import java.util.List;
import java.util.Optional;

public interface ChatbotSessionRepository extends JpaRepository<ChatbotSession, Long> {
    Optional<ChatbotSession> findByUserIdAndAudioFileId(String userId, Long audioFileId);
    List<ChatbotSession> findByUserIdAndSessionStatus(String userId, SessionStatus status);
}
