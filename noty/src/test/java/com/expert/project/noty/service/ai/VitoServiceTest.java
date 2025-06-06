package com.expert.project.noty.service.ai;

import com.expert.project.noty.dto.ai.currentserver.TranscribeResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.util.List;

@SpringBootTest
@Transactional
public class VitoServiceTest {
    @Autowired
    private VitoService vitoService;
    @Autowired
    private GeminiService geminiService;

    @Test
    public void VitoTest() {
        File file = new File("/Users/jinjin/Downloads/meeting_test.mp3");

        String token = vitoService.extractAccessToken(vitoService.authenticate());

        System.out.println(token);
        String transcribeId = vitoService.extractId(vitoService.transcribe(token, file));
        System.out.println(transcribeId);

        String status;
        TranscribeResult transcribeJson;
        do {
            transcribeJson = vitoService.getTranscriptionResult(token, transcribeId);
            status = transcribeJson.getStatus();
            try {
                Thread.sleep(1000); // 1초 대기 (1000 밀리초)
            } catch (InterruptedException e) {
                e.printStackTrace(); // 예외 처리
            }
        } while (status.equals("transcribing"));

        TranscribeResult.Results results = transcribeJson.getResults();

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

        System.out.println(finalResult);
    }
}
