package com.expert.project.noty.controller.ai;

import com.expert.project.noty.dto.ai.currentserver.SavedFileRequest;
import com.expert.project.noty.dto.ai.currentserver.SummationResponse;
import com.expert.project.noty.dto.auth.CustomUserDetails;
import com.expert.project.noty.service.ai.STTService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class STTController {

    private final STTService sttService;

    public STTController(STTService sttService) {
        this.sttService = sttService;
    }

    @PostMapping("/stt")
    public ResponseEntity<SummationResponse> getSummation(
            @RequestParam("savedFileName") String savedFileName,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        System.out.println("hellow");
        String userId = userDetails.getUsername();

        SummationResponse response = sttService.getSummationBySavedFileName(savedFileName, userId);
        return ResponseEntity.ok(response);
    }
}
