package com.expert.project.noty.service.ai;

import com.expert.project.noty.util.ai.MultipartInputStreamFileResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class WhisperService {

    @Value("${openai.api.key}")
    private String apiKey;

    String apiUrl = "http://203.255.81.132:10100/whisper";

    public CompletableFuture<String> transcribe(File audioFile) {
        try {
            final RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // 파일을 ByteArrayResource로 변환
            ByteArrayResource resource = new ByteArrayResource(new FileInputStream(audioFile).readAllBytes()) {
                @Override
                public String getFilename() {
                    return audioFile.getName();
                }
            };

            body.add("file", resource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, requestEntity, Map.class);
            Map<String, Object> responseBody = response.getBody();

            // 응답에서 텍스트를 추출하고 CompletableFuture로 반환
            if (responseBody != null && responseBody.containsKey("text")) {
                return CompletableFuture.completedFuture((String) responseBody.get("text"));
            } else {
                return CompletableFuture.failedFuture(new RuntimeException("No text found in the response"));
            }

        } catch (IOException e) {
            throw new RuntimeException("Whisper 호출 실패", e);
        }
    }

}
