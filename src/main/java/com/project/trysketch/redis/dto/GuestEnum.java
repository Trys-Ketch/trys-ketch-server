package com.project.trysketch.redis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GuestEnum {
    GUEST_NAME_KEY("guest"),
    GUEST_NICKNAME_KEY("nickname");

    private final String key;
}
