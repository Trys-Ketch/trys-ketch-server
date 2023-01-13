package com.project.trysketch.dto.response;

import com.project.trysketch.entity.GameFlow;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KeywordResponseDto {
    private String keyword;

    public KeywordResponseDto(GameFlow gameFlow) {
        this.keyword = gameFlow.getKeyword();
    }
}

