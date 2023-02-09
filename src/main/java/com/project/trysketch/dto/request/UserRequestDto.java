package com.project.trysketch.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import javax.validation.constraints.Pattern;

// 1. 기능   : 유저 닉네임 입력 요소
// 2. 작성자 : 서혁수, 황미경
@Getter
@AllArgsConstructor
@Builder
public class UserRequestDto {

    @Pattern(regexp = "^[a-zA-Z0-9가-힣 ]{2,12}$", message = "닉네임은 2자이상, 12자이하이어야하며 한글, 대소문자, 숫자, 띄어쓰기만 가능합니다.")
    private String nickname;

    private String imgUrl;

}
