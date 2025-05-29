package com.expert.project.noty.dto.file;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AudioNameModifyRespond {

    private String savedName;

    public AudioNameModifyRespond(String savedName) {
        this.savedName = savedName;
    }
}
