package com.project.trysketch.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 1. 기능    : 게임 진행 중 입력 요소
// 2. 작성자  : 김재영, 서혁수, 안은솔, 황미경
@Getter
@AllArgsConstructor
@Builder
public class GameFlowRequestDto {

    private int round;           // 진행 라운드
    private int keywordIndex;    // 키워드 순번
    private Long roomId;         // 방번호
    private Long timeLimit;      // 라운드 시간 조절
    private String keyword;      // 제시어
    private String imagePath;    // 이미지 URL
    private String token;        // 회원 & 비회원 토큰
    private String webSessionId; // Socket 연결 시 생성되는 유저의 UUID
    private String image;        // 이미지 byte code
    private String difficulty;   // 난이도 조절
    private boolean isSubmitted; // 유저의 제출 여부
}
