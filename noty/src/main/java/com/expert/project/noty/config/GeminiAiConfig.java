package com.expert.project.noty.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Setter
@Getter
@PropertySource("classpath:geminiAi.properties")
@Component
@ConfigurationProperties(prefix = "gemini")
public class GeminiAiConfig {

    private String apiKey;
}
