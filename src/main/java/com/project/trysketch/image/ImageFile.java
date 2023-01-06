package com.project.trysketch.image;

import com.project.trysketch.user.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@Entity
@NoArgsConstructor
public class ImageFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                     // id

    @Column(nullable = false)            // image 경로
    private String path;

    @Column(nullable = false)            // 그림 그린 사람
    private String painter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;                   // 좋아요 누른 사람

    //생성자
    public ImageFile(String path, User user, String painter) {
        this.path = path;
        this.user = user;
        this.painter = painter;
    }
}