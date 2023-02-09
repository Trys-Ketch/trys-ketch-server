package com.project.trysketch.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import static com.project.trysketch.global.exception.StatusMsgCode.BAD_ID_PASSWORD;

// 1. 기능    : Global 예외처리 핸들러
// 2. 작성자  : 안은솔
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

//    @ExceptionHandler(value = { ConstraintViolationException.class, DataIntegrityViolationException.class })
//    protected ResponseEntity<ErrorResponse> handleDataException() {
//        log.error("handleDataException throw Exception : {}", DUPLICATE_RESOURCE);
//        return ErrorResponse.toResponseEntity(DUPLICATE_RESOURCE);
//    }

    // custom 예외 처리
    @ExceptionHandler(value = { CustomException.class })
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.error("handleCustomException throw CustomException : {}", e.getStatusMsgCode());
        return ErrorResponse.toResponseEntity(e.getStatusMsgCode());
    }

    // validation 예외 처리
    @ExceptionHandler(value = { MethodArgumentNotValidException.class })
    protected ResponseEntity<ErrorResponse> processValidationException(MethodArgumentNotValidException e) {
        log.error("handleCustomException throw CustomException : {}", e.getMessage());
        return ErrorResponse.toResponseEntity(BAD_ID_PASSWORD);
    }

}
