package com.project.trysketch.entity;

import com.project.trysketch.global.utill.Timestamped;
import lombok.*;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

// 1. 기능   : 게임 방 엔티티
// 2. 작성자 : 김재영
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class GameRoom extends Timestamped {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(nullable = false)
    private String title;                  // 방 제목

    @Column(nullable = false)
    private Long hostId;                   // 방장 userId

    @Column(nullable = false)
    private String hostNick;               // 방장 nickname

    @Column(nullable = false)
    private String randomCode;             // 랜덤코드

    @Column
    private Integer roundMaxNum;           // 최종 라운드수

    @Column(nullable = false)
    @Builder.Default
    private String difficulty = "easy";    // 게임 난이도

    @Column(nullable = false)
    @Builder.Default
    private Long timeLimit = 60000L;       // 라운드별 제한 시간

    @Column(nullable = false)
    @Builder.Default
    private int resultCount = 0;           // 결과창 배열 키워드 순번

    @Column(nullable = false)
    @Builder.Default
    private boolean isPlaying = false;     // 게임 진행여부

    @OneToMany(mappedBy = "gameRoom", fetch = FetchType.EAGER)
    @Builder.Default
    private List<GameRoomUser> gameRoomUserList = new ArrayList<>();    // 게임룸의 유저리스트

    public void GameRoomStatusUpdate(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    public void RoundMaxNumUpdate(int gamerNum) {
        this.roundMaxNum = gamerNum;
    }

    public void difficultyUpdate(String difficulty) {
        this.difficulty = difficulty;
    }

    public void timeLimitUpdate(Long timeLimit) {
        this.timeLimit = timeLimit;
    }

    public void GameRoomUpdate(Long hostId, String hostNick) {
        this.hostId = hostId;
        this.hostNick = hostNick;
    }

    public void update(int resultCount) {
        this.resultCount = resultCount;
    }
}
