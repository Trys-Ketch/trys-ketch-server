package com.project.trysketch.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.persistence.*;

// 1. 기능   : ImageLike 요소
// 2. 작성자 : 황미경
@Entity
@Getter
@NoArgsConstructor
public class ImageLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)              // 좋아요 눌린 이미지
    private Image image;

    @ManyToOne
    @JoinColumn(nullable = false)             // 좋아요 누른 유저
    private User user;


    public ImageLike(Image image, User user) {
        this.image = image;
        this.user = user;
    }
}
