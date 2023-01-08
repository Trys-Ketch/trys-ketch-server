package com.project.trysketch.image;

import com.project.trysketch.user.entity.User;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

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
