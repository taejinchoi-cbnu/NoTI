package com.expert.project.noty.controller.ai;

import com.expert.project.noty.dto.ai.currentserver.ChatConversationDTO;
import com.expert.project.noty.dto.ai.currentserver.ChatMessageRequest;
import com.expert.project.noty.dto.ai.currentserver.ChatMessageResponse;
import com.expert.project.noty.dto.ai.currentserver.SessionResponse;
import com.expert.project.noty.dto.auth.CustomUserDetails;
import com.expert.project.noty.entity.ChatConversation;
import com.expert.project.noty.entity.ChatbotSession;
import com.expert.project.noty.service.ai.ChatbotService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {
    private final ChatbotService chatbotService;

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    // 챗봇 세션 생성/조회
    @PostMapping("/session")
    public ResponseEntity<?> createOrGetSession(
            @RequestParam String savedFileName,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUsername();
        ChatbotSession session = chatbotService.getOrCreateSession(userId, savedFileName);

        return ResponseEntity.ok(new SessionResponse(
                session.getId(),
                session.getSessionStatus().toString()
        ));
    }

    // 채팅 메시지 전송
    @PostMapping("/message")
    public ResponseEntity<?> sendMessage(
            @RequestPart("sessionId") String sessionId,
            @RequestPart("message") String message,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        try {
            String response = chatbotService.processChatMessage(
                    Long.parseLong(sessionId),
                    message
            );

            return ResponseEntity.ok(new ChatMessageResponse(response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("메시지 처리 중 오류가 발생했습니다.");
        }
    }

    // 채팅 기록 조회
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<?> getChatHistory(@PathVariable Long sessionId) {
        List<ChatConversationDTO> history = chatbotService.getAllConversationsBySession(sessionId);
        return ResponseEntity.ok(history);
    }
}
