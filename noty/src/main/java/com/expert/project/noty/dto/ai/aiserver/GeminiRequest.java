package com.expert.project.noty.dto.ai.aiserver;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GeminiRequest {
    public List<Content> contents;

    public static class Content {
        public List<Part> parts;
    }

    public static class Part {
        public String text;
    }
}