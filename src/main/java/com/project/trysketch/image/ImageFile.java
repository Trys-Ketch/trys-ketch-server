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
public class ImageFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                     // id

    @Column(nullable = false)            // image 경로
    private String path;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "painter_id", nullable = false)
    private User user;                   // 그림그린 사람

    //생성자
    public ImageFile(String path, User user) {
        this.path = path;
        this.user = user;
    }
}