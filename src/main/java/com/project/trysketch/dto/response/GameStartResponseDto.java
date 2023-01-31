package com.project.trysketch.dto.response;

import lombok.*;
@Getter
@Setter
@Builder
@AllArgsConstructor
public class GameStartResponseDto {

    private Long gameRoomId;
    private Integer roundTimeout;
//    private String mode;
}
