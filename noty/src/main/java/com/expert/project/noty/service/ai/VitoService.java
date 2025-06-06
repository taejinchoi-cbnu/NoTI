package com.expert.project.noty.service.ai;

import com.expert.project.noty.dto.ai.currentserver.TranscribeResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Service
public class VitoService {

    @Value("${vito.key.id}")
    private String apiId;
    @Value("${vito.key.password}")
    private String apiPassword;

    private final RestTemplate restTemplate;
    private final GeminiService geminiService;

    public VitoService(RestTemplateBuilder restTemplateBuilder,
                       GeminiService geminiService) {
        this.restTemplate = restTemplateBuilder.build();
        this.geminiService = geminiService;
    }

    public String authenticate() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        String body = "client_id=" + URLEncoder.encode(apiId, StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(apiPassword, StandardCharsets.UTF_8);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    "https://openapi.vito.ai/v1/authenticate", request, String.class);
            return response.getBody();
        } catch (RestClientException e) {
            throw new RuntimeException("Failed to authenticate with Vito API", e);
        }
    }

    public String extractAccessToken(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonResponse);
            return rootNode.get("access_token").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON and extract token", e);
        }
    }

    public String transcribe(String jwtToken, File audioFile) {
        String url = "https://openapi.vito.ai/v1/transcribe";

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(jwtToken);

        // 요청 본문 설정 (파일 + config JSON)
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(audioFile));
        body.add("config", getConfigJson());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to transcribe audio file", e);
        }
    }

    private HttpEntity<String> getConfigJson() {
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        String configJson = "{\n" +
                "   \"use_diarization\": true,\n" +
                "   \"diarization\": {\"spk_count\": 2},\n" +
                "   \"use_itn\": false,\n" +
                "   \"use_disfluency_filter\": false,\n" +
                "   \"use_profanity_filter\": false,\n" +
                "   \"use_paragraph_splitter\": true,\n" +
                "   \"paragraph_splitter\": {\"max\": 50}\n" +
                "}";
        return new HttpEntity<>(configJson, jsonHeaders);
    }

    public String extractId(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonResponse);
            return rootNode.get("id").asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract 'id' from JSON", e);
        }
    }

    public TranscribeResult getTranscriptionResult(String jwtToken, String transcribeId) {
        String url = "https://openapi.vito.ai/v1/transcribe/" + transcribeId;

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setBearerAuth(jwtToken);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<TranscribeResult> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    TranscribeResult.class  // <- 여기가 핵심
            );
            return response.getBody(); // JSON -> TranscribeResult로 자동 변환됨
        } catch (Exception e) {
            throw new RuntimeException("Failed to get transcription result", e);
        }
    }

    public TranscribeResult waitUntilTranscriptionComplete(String token, String transcribeId) {
        int retryCount = 0;
        int maxRetries = 10;  // 최대 10회
        long waitTime = 1000; // 초기 대기 1초

        while (retryCount < maxRetries) {
            TranscribeResult result = getTranscriptionResult(token, transcribeId);
            String status = result.getStatus();

            if (!"transcribing".equalsIgnoreCase(status)) {
                return result;
            }

            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for transcription", e);
            }

            retryCount++;
            waitTime = Math.min(waitTime * 2, 10000); // 최대 10초까지 증가
        }

        throw new RuntimeException("Transcription did not complete within expected time");
    }

    public String getScriptResult(File audioFile) {

        String token = extractAccessToken(authenticate());
        String transcribeId = extractId(transcribe(token, audioFile));

        TranscribeResult transcribeResult = waitUntilTranscriptionComplete(token, transcribeId);

        TranscribeResult.Results results = transcribeResult.getResults();
        List<TranscribeResult.Utterance> utterances = results.getUtterances();

        StringBuilder resultBuilder = new StringBuilder();

        for (TranscribeResult.Utterance utterance : utterances) {
            int start = utterance.getStart_at();
            int end = start + utterance.getDuration();
            String msg = utterance.getMsg();

            resultBuilder
                    .append("{")
                    .append(start).append(", ")
                    .append(end).append(", ")
                    .append(msg)
                    .append("}, ");
        }

        if (resultBuilder.length() > 2) {
            resultBuilder.setLength(resultBuilder.length() - 2);
        }

        String beforeParsing = resultBuilder.toString();

        String defaultPrompt = """
                당신은 녹음 파일 스크립트를 정리하는 역할입니다.
                [텍스트 형식] {start, end, massage} (시간은 ms단위)
                [출력물]
                [대화 시작 시간(mm:ss) ~ 대화 끝 시간(mm:ss)]
                내용1
                
                [대화 시작 시간(mm:ss) ~ 대화 끝 시간(mm:ss)]
                내용2...
                [조건] 출력물을 만들 때 틀린 부분이 있으면 자연스럽게 바꾸기. (중요!)대화 시작에서 대화 끝 부분은 주어진 스크립트 기반이 아니라 스스로 유추하여 출력물 내용이 마침표로 끝나야 함. 
                [내용]
                """;
        String prompt = defaultPrompt + beforeParsing;

        String finalResult = geminiService.callGemini(prompt);

        return finalResult;
    }
}
