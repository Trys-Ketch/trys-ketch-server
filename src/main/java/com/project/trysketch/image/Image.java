package com.project.trysketch.image;

import com.project.trysketch.user.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;


// 1. 기능   : ImageFile 구성요소
// 2. 작성자 : 황미경

@Getter
@Entity
@NoArgsConstructor
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                     // id

    @Column(nullable = false)            // image 경로
    private String path;

    @Column(nullable = false)            // 그린사람의 닉네임
    private String painter;

    //생성자
    public Image(String path, String painter) {
        this.path = path;
        this.painter = painter;
    }
}