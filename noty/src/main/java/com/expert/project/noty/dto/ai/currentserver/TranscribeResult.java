package com.expert.project.noty.dto.ai.currentserver;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TranscribeResult {
    private String id;
    private String status;
    private Results results;

    @Getter
    @Setter
    public static class Results {
        private List<Utterance> utterances;
    }

    @Getter
    @Setter
    public static class Utterance {
        private int start_at;
        private int duration;
        private String msg;
        private int spk;
        private String lang;
    }
}
