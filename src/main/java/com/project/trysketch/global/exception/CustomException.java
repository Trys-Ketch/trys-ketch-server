package com.project.trysketch.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 1. 기능    : 커스텀 예외처리
// 2. 작성자  : 안은솔
@Getter
@AllArgsConstructor
public class CustomException extends RuntimeException{
    private final StatusMsgCode statusMsgCode;
}