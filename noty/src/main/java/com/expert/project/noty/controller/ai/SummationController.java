package com.expert.project.noty.controller.ai;

import com.expert.project.noty.dto.ai.currentserver.GeminiPromptRequest;
import com.expert.project.noty.dto.ai.currentserver.STTResponse;
import com.expert.project.noty.dto.ai.currentserver.SummationResponse;
import com.expert.project.noty.dto.auth.CustomUserDetails;
import com.expert.project.noty.service.ai.SummationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/ai")
public class SummationController {

    private final SummationService summationService;

    public SummationController(SummationService summationService) {
        this.summationService = summationService;
    }

    @PostMapping("/gemini")
    public ResponseEntity<SummationResponse> handleGeminiRequest(
            @RequestParam("savedFileName") String savedFileName,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        String userId = userDetails.getUsername();

        SummationResponse response = summationService.processGeminiRequest(userId, savedFileName);
        return ResponseEntity.ok(response);
    }
}
