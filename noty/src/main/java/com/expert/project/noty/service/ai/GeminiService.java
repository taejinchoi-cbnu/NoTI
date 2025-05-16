package com.expert.project.noty.service.ai;

import com.expert.project.noty.dto.ai.aiserver.GeminiRequest;
import com.expert.project.noty.dto.ai.aiserver.GeminiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    public String callGemini(String prompt) {
        final String URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + apiKey;

        // 요청 객체 구성
        GeminiRequest request = new GeminiRequest();
        GeminiRequest.Part part = new GeminiRequest.Part();
        part.text = prompt;

        GeminiRequest.Content content = new GeminiRequest.Content();
        content.parts = List.of(part);

        request.contents = List.of(content);

        // HTTP 헤더
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 요청 보내기
        HttpEntity<GeminiRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<GeminiResponse> response = restTemplate.postForEntity(URL, entity, GeminiResponse.class);

        // 응답 처리
        GeminiResponse geminiResponse = response.getBody();
        if (geminiResponse != null && geminiResponse.candidates != null && !geminiResponse.candidates.isEmpty()) {
            return geminiResponse.candidates.get(0).content.parts.get(0).text;
        }

        return "응답 없음";
    }

    @Async
    public CompletableFuture<String> sendRequestAsync(String prompt) {

        String response = restTemplate.postForObject(
                "http://your-ai-server.com/generate",
                null,
                String.class
        );

        return CompletableFuture.completedFuture(response);
    }
}
