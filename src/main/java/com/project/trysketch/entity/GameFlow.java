package com.project.trysketch.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.*;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class GameFlow {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private long id;

    @Column
    private int roundId;

    @Column
    private int KeywordId;



    @Column(nullable = false)
    private Long gameId;

    @ManyToOne
    private GameInfo gameInfo;

    public GameFlow(int roundId, int keywordId) {
        this.roundId = roundId;
        this.KeywordId = keywordId;
        this.gameId = gameInfo.getId();
    }

    //    그림 테이블 : (user's nickname), roundId, keyword's Id, gameUd
//    제시어 테이블:                   roundId, keyword's Id, gameid




//            [ [제시어, 그림, 제시어(5번유저) ],
//            [제시어, 그림, 제시어(4번유저), 제시어, 그림, 제시어],
//            [제시어, 그림, 제시어(5번유저) ],
//            [제시어, 그림, 제시어(1번유저) ],
//            [제시어, 그림, 제시어(2번유저), 그림, 제시어 ]]
//



}
