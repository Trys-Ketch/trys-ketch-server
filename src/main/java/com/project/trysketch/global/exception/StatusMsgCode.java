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
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다"),
    EXIST_USER(HttpStatus.BAD_REQUEST, "중복된 이메일입니다"),
    EXIST_NICK(HttpStatus.BAD_REQUEST, "중복된 닉네임입니다"),
    BAD_ID_PASSWORD(HttpStatus.BAD_REQUEST, "형식이 맞지 않습니다"),
    INVALID_AUTH_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다"),
    NECESSARY_LOG_IN(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다"),
    NOT_FOUND_NICK(HttpStatus.BAD_REQUEST, "닉네임을 찾을 수 없습니다"),
    HISTORY_NOT_FOUND(HttpStatus.BAD_REQUEST, "활동 이력을 찾울 수 없습니다"),


    IMAGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "이미지를 찾을 수 없습니다"),
    ALREADY_CLICKED_LIKE(HttpStatus.BAD_REQUEST, "이미 좋아요를 눌렀습니다"),
    ALREADY_CANCEL_LIKE(HttpStatus.BAD_REQUEST, "이미 좋아요 취소를 눌렀습니다"),


    ALREADY_PLAYING(HttpStatus.BAD_REQUEST, "게임이 이미 시작되었습니다"),
    FULL_BANG(HttpStatus.BAD_REQUEST, "허용된 최대 사용자 수에 도달했습니다"),
    GAMEROOM_NOT_FOUND(HttpStatus.BAD_REQUEST,"방을 찾을 수 없습니다"),
    GAMEFLOW_NOT_FOUND(HttpStatus.BAD_REQUEST,"게임 플로우를 찾을 수 없습니다"),
    HOST_AUTHORIZATION_NEED(HttpStatus.BAD_REQUEST,"방장 권한이 필요합니다"),
    GAME_NOT_ONLINE(HttpStatus.BAD_REQUEST,"진행되는 게임이 없습니다"),
    NOT_STARTED_YET(HttpStatus.BAD_REQUEST,"아직 게임이 시작되지 않았습니다"),
    KEYWORD_INDEX_NOT_FOUND(HttpStatus.BAD_REQUEST, "불러올 키워드 순번이 없습니다"),


    /* Resource 의 현재 상태와 충돌 관련 Status 메시지 코드*/
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "데이터가 이미 존재합니다"),
    DUPLICATE_USER(HttpStatus.CONFLICT, "유저가 이미 존재합니다"),
    ONE_MAN_ONE_ROOM(HttpStatus.CONFLICT, "이미 접속중인 방이 있습니다"),


    /* 정상 처리 관련 Status 메시지 코드 */
    OK(HttpStatus.OK,"요청이 정상적으로 처리되었습니다"),
    SUCCESS_ENTER_GAME(HttpStatus.OK,"성공적으로 방에 입장하셨습니다"),
    SUCCESS_EXIT_GAME(HttpStatus.OK,"성공적으로 방에서 퇴장하셨습니다"),
    LOG_IN(HttpStatus.OK, "로그인에 성공했습니다"),
    CANCEL_LIKE(HttpStatus.OK, "좋아요 취소"),
    LIKE_IMAGE(HttpStatus.OK, "좋아요 완료"),
    DONE_DRAWING(HttpStatus.OK, "사진 저장 완료"),
    DELETE_IMAGE(HttpStatus.OK, "사진 삭제 완료"),
    END_GAME(HttpStatus.OK, "게임이 정상 종료되었습니다"),
    SHUTDOWN_GAME(HttpStatus.OK," 게임이 비정상 종료되었습니다"),
    START_GAME(HttpStatus.OK, "게임이 시작되었습니다"),
    SUBMIT_IMAGE_DONE(HttpStatus.OK, "이미지 제출이 되었습니다"),
    SUBMIT_KEYWORD_DONE(HttpStatus.OK, "단어 제출이 되었습니다"),
    CREATE_ROOM(HttpStatus.OK, "방을 만들었습니다");




    private final HttpStatus httpStatus;
    private final String detail;
}

