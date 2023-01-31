package com.project.trysketch.dto.response;

import com.project.trysketch.entity.GameFlow;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ImageResponseDto {
    private String ImagePath;

    public ImageResponseDto(GameFlow gameFlow) {
        this.ImagePath = gameFlow.getImagePath();
    }
}

