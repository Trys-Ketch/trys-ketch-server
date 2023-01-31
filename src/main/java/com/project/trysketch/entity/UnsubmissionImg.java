package com.project.trysketch.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class UnsubmissionImg {
    @Id
    private Integer id;

    @Column
    private String unsubmissionImg;
}
