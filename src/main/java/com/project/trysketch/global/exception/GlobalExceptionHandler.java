package com.project.trysketch.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import static com.project.trysketch.global.exception.StatusMsgCode.BAD_ID_PASSWORD;
import static com.project.trysketch.global.exception.StatusMsgCode.DUPLICATE_RESOURCE;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = { ConstraintViolationException.class, DataIntegrityViolationException.class})
    protected ResponseEntity<ErrorResponse> handleDataException() {
        log.error("handleDataException throw Exception : {}", DUPLICATE_RESOURCE);
        return ErrorResponse.toResponseEntity(DUPLICATE_RESOURCE);
    }

    @ExceptionHandler(value = { CustomException.class })
    protected ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.error("handleCustomException throw CustomException : {}", e.getStatusMsgCode());
        return ErrorResponse.toResponseEntity(e.getStatusMsgCode());
    }

    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    protected ResponseEntity<ErrorResponse> processValidationException(MethodArgumentNotValidException e) {
        log.error("handleCustomException throw CustomException : {}", e.getMessage());
        return ErrorResponse.toResponseEntity(BAD_ID_PASSWORD);
    }

}
