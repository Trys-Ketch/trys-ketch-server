package com.project.trysketch.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

// 1. 기능    : Global 메시지 종류
// 2. 작성자  : 안은솔
@Getter
@AllArgsConstructor
public enum StatusMsgCode {

    /* 회원, 비회원 관련 Status 메시지 코드 */
    USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "회원을 찾을 수 없습니다"),
    GAME_ROOM_USER_NOT_FOUND(HttpStatus.BAD_REQUEST, "플레이어를 찾을 수 없습니다"),
    BAD_ID_PASSWORD(HttpStatus.BAD_REQUEST, "형식이 맞지 않습니다"),
    INVALID_AUTH_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다"),
    NECESSARY_LOG_IN(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다"),
    NOT_FOUND_NICK(HttpStatus.BAD_REQUEST, "닉네임을 찾을 수 없습니다"),
    HISTORY_NOT_FOUND(HttpStatus.BAD_REQUEST, "활동 이력을 찾울 수 없습니다"),
    MINMAX_ROUND_TIME(HttpStatus.BAD_REQUEST, "요청한 라운드 시간의 최소/최대치에 도달해 변경이 불가능합니다"),

    /* 이미지 관련 Status 메시지 코드 */
    IMAGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "이미지를 찾을 수 없습니다"),
    IMAGE_SAVE_FAILED(HttpStatus.BAD_REQUEST,"이미지 저장에 실패하였습니다"),

    /* 게임 관련 Status 메시지 코드 */
    ALREADY_PLAYING(HttpStatus.BAD_REQUEST, "게임이 이미 시작되었습니다"),
    FULL_BANG(HttpStatus.BAD_REQUEST, "허용된 최대 사용자 수에 도달했습니다"),
    GAMEROOM_NOT_FOUND(HttpStatus.BAD_REQUEST,"방을 찾을 수 없습니다"),
    GAMEFLOW_NOT_FOUND(HttpStatus.BAD_REQUEST,"게임 플로우를 찾을 수 없습니다"),
    HOST_AUTHORIZATION_NEED(HttpStatus.BAD_REQUEST,"방장 권한이 필요합니다"),
    GAME_NOT_ONLINE(HttpStatus.BAD_REQUEST,"진행되는 게임이 없습니다"),
    NOT_STARTED_YET(HttpStatus.BAD_REQUEST,"아직 게임이 시작되지 않았습니다"),
    KEYWORD_INDEX_NOT_FOUND(HttpStatus.BAD_REQUEST, "불러올 키워드 순번이 없습니다"),

    /* Resource 의 현재 상태와 충돌 관련 Status 메시지 코드*/
//    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "데이터가 이미 존재합니다"),

    /* 정상 처리 관련 Status 메시지 코드 */
    OK(HttpStatus.OK,"요청이 정상적으로 처리되었습니다"),
    LOG_IN(HttpStatus.OK, "로그인에 성공했습니다"),
    CANCEL_LIKE(HttpStatus.OK, "좋아요 취소"),
    LIKE_IMAGE(HttpStatus.OK, "좋아요 완료"),
    UPDATE_USER_PROFILE(HttpStatus.OK, "프로필이 변경되었습니다");

    private final HttpStatus httpStatus;
    private final String detail;
}

