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
    @JoinColumn(name = "IMAGEFILE_ID", nullable = false)
    private Image image;

    @ManyToOne
    @JoinColumn(name = "USER_ID", nullable = false)
    private User user;

    // 생성자
    public ImageLike(Image image, User user) {
        this.image = image;
        this.user = user;
    }
}
