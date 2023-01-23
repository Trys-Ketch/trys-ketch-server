package com.project.trysketch.dto.request;

import lombok.Getter;
import javax.validation.constraints.Pattern;

// 1. 기능   : 비회원 닉네임 요청값
// 2. 작성자 : 서혁수
@Getter
public class GuestNickRequestDto {

    @Pattern(regexp = "^[a-zA-Z0-9가-힣 ]{2,12}$", message = "닉네임은 2자이상, 12자이하이어야하며 한글, 대소문자, 숫자, 띄어쓰기만 가능합니다.")
    private String nickname;

    private String imgUrl;
}
