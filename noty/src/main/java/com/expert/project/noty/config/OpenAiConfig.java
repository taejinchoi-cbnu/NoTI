package com.expert.project.noty.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Getter
@Setter
@PropertySource("classpath:openAi.properties")
@Component
@ConfigurationProperties(prefix = "openai")
public class OpenAiConfig {

    private String apiKey;
}
