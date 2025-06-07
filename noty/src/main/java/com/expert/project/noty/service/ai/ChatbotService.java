package com.expert.project.noty.service.ai;

import com.expert.project.noty.entity.*;
import com.expert.project.noty.repository.AudioFileRepository;
import com.expert.project.noty.repository.ChatConversationRepository;
import com.expert.project.noty.repository.ChatbotSessionRepository;
import com.expert.project.noty.repository.SummationRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.expert.project.noty.dto.ai.currentserver.ChatConversationDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatbotService {
    private final ChatbotSessionRepository sessionRepository;
    private final ChatConversationRepository conversationRepository;
    private final GeminiService geminiService;
    private final STTService sttService;
    private final AudioFileRepository audioFileRepository;
    private final ChatbotSessionRepository chatbotSessionRepository;
    private final ChatConversationRepository chatConversationRepository;
    private final SummationRepository summationRepository;

    public ChatbotService(ChatbotSessionRepository sessionRepository,
                          ChatConversationRepository conversationRepository,
                          GeminiService geminiService,
                          STTService sttService,
                          AudioFileRepository audioFileRepository,
                          ChatbotSessionRepository chatbotSessionRepository,
                          ChatConversationRepository chatConversationRepository,
                          SummationRepository summationRepository) {
        this.sessionRepository = sessionRepository;
        this.conversationRepository = conversationRepository;
        this.geminiService = geminiService;
        this.sttService = sttService;
        this.audioFileRepository = audioFileRepository;
        this.chatbotSessionRepository = chatbotSessionRepository;
        this.chatConversationRepository = chatConversationRepository;
        this.summationRepository = summationRepository;
    }

    // 챗봇 세션 생성 또는 조회
    public ChatbotSession getOrCreateSession(String userId, String savedFileName) {
        AudioFileEntity audioFile = audioFileRepository.findBySavedName(savedFileName)
                .orElseThrow(() -> new RuntimeException("Audio file not found"));

        Long audioFileId = audioFile.getId();

        return sessionRepository.findByUserIdAndAudioFileId(userId, audioFileId)
                .orElseGet(() -> createNewSession(userId, audioFileId));
    }

    @Async
    public ChatbotSession createNewSession(String userId, Long audioFileId) {

        AudioFileEntity audioFile = audioFileRepository.findById(audioFileId)
                .orElseThrow(() -> new RuntimeException("Audio file not found"));

        // 1. 세션 생성
        ChatbotSession session = new ChatbotSession();
        session.setUserId(userId);
        session.setAudioFile(audioFile);
        session.setSessionStatus(SessionStatus.INITIALIZING);
        session = sessionRepository.save(session);

        try {
            String sttContent = sttService.getSTTByAudioId(audioFileId);

            session.setFullContext(sttContent);
            session.setSessionStatus(SessionStatus.READY);
            sessionRepository.save(session);

        } catch (Exception e) {
            session.setSessionStatus(SessionStatus.ERROR);
            sessionRepository.save(session);
        }

        return session;
    }

    // 채팅 메시지 처리
    public String processChatMessage(Long sessionId, String userMessage) {
        // 1. 사용자 메시지 저장
        saveConversation(sessionId, userMessage, true);

        // 2. AI 응답 생성
        String aiResponse = generateAIResponse(sessionId, userMessage);

        // 3. AI 응답 저장
        saveConversation(sessionId, aiResponse, false);

        return aiResponse;
    }

    private void saveConversation(Long sessionId, String message, boolean isUser) {
        ChatbotSession session = chatbotSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));

        ChatConversation conversation = new ChatConversation();
        conversation.setSession(session);
        conversation.setMessage(message);
        conversation.setIsUserMessage(isUser);
        conversation.setCreatedAt(LocalDateTime.now());

        conversationRepository.save(conversation);
    }

    private String generateAIResponse(Long sessionId, String userMessage) {
        // 1. 이전 대화 이력 불러오기
        List<ChatConversation> history = chatConversationRepository
                .findBySessionIdOrderByCreatedAtAsc(sessionId);

        ChatbotSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found with ID: " + sessionId));

        AudioFileEntity audioFileEntity = session.getAudioFile();
        SummationEntity summationEntity = summationRepository.findByAudioId(audioFileEntity.getId())
                .orElseThrow(() -> new RuntimeException("Summation is not found"));

        String audioStt = summationEntity.getStt();

        // 2. 프롬프트 구성
        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                [역할] 당신은 AI 챗봇입니다.
                [출력] 유저가 편안함을 느낄 수 있는 출력물
                [제약] 원래 있던 내용을 기반으로 답장을 해줘야 합니다. 없는 내용이라면 관련 내용이 없다고 표시해야 합니다. 이모지는 절대 없이 답변해야 합니다.
                [기타] 유저가 관련 내용에 대해 궁금해 한다면 답변해야 합니다.
                [내용]
                """);
        prompt.append(audioStt).append("\n");
        prompt.append("[이전 기록]\n");
        for (ChatConversation chat : history) {
            if (chat.getIsUserMessage()) {
                prompt.append("User: ").append(chat.getMessage()).append("\n");
            } else {
                prompt.append("AI: ").append(chat.getMessage()).append("\n");
            }
        }
        prompt.append("User: ").append(userMessage).append("\nAI: ");

        // 3. LLM API 호출
        String aiResponse = geminiService.callGemini(prompt.toString());

        return aiResponse;
    }

    public List<ChatConversationDTO> getAllConversationsBySession(Long sessionId) {
        List<ChatConversation> entities = chatConversationRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        return entities.stream()
                .map(ChatConversationDTO::new)
                .collect(Collectors.toList());
    }
}
