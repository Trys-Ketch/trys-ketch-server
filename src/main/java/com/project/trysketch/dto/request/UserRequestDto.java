package com.project.trysketch.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class UserRequestDto {

    private String nickname;
    private String imgUrl;

}
