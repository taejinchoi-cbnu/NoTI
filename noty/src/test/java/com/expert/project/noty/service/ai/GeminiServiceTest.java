package com.expert.project.noty.service.ai;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.expert.project.noty.service.ai.GeminiService.*;

import java.io.IOException;

@SpringBootTest
@Transactional
public class GeminiServiceTest {

    @Autowired
    private GeminiService geminiService;

    @Test
    public void Test() {
        String result = geminiService.callGemini("안녕하세요");

        System.out.println(result);
    }
}
