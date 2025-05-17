package com.expert.project.noty.controller.ai;

import com.expert.project.noty.dto.ai.currentserver.STTResponse;
import com.expert.project.noty.dto.auth.CustomUserDetails;
import com.expert.project.noty.service.ai.STTService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/ai")
public class STTController {

    private final STTService sttService;

    public STTController(STTService sttService) {
        this.sttService = sttService;
    }

    // 디버깅 위해서 print 몇개 추가 (태진)
    @PostMapping("/stt")
    public ResponseEntity<STTResponse> getSummation(
            @RequestParam("savedFileName") String savedFileName,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        System.out.println("hellow");
        System.out.println("savedFileName: " + savedFileName);
        System.out.println("userDetails: " + (userDetails != null ? "not null" : "null"));
        if (userDetails != null) {
            System.out.println("userId: " + userDetails.getUsername());
            System.out.println("userRole: " + userDetails.getAuthorities());
        }

        try {
            String userId = userDetails.getUsername();
            System.out.println("Calling service with userId: " + userId);

            STTResponse response = sttService.getSummationBySavedFileName(savedFileName, userId);
            System.out.println("Service returned successfully: " + (response != null));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.out.println("Error in controller: " + e.getMessage());
            e.printStackTrace();
            // 클라이언트에게도 오류 정보 반환 (개발 중에만)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new STTResponse("Error: " + e.getMessage()));
        }
    }
}
