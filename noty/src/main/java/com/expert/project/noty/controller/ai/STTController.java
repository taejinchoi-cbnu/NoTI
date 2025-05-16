package com.expert.project.noty.controller.ai;

import com.expert.project.noty.dto.ai.currentserver.STTResponse;
import com.expert.project.noty.dto.auth.CustomUserDetails;
import com.expert.project.noty.service.ai.STTService;
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
    public ResponseEntity<STTResponse> getSummation(
            @RequestParam("savedFileName") String savedFileName,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        System.out.println("hellow");
        String userId = userDetails.getUsername();

        STTResponse response = sttService.getSummationBySavedFileName(savedFileName, userId);
        return ResponseEntity.ok(response);
    }
}
