package com.expert.project.noty.repository;

import com.expert.project.noty.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
    List<ChatConversation> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
    List<ChatConversation> findTop10BySessionIdOrderByCreatedAtDesc(Long sessionId);
}
