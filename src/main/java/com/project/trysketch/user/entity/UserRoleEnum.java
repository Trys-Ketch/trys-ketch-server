package com.project.trysketch.user.entity;

// 1. 기능   : 유저 권한 구성요소
// 2. 작성자 : 서혁수
public enum UserRoleEnum {
    USER(Authority.USER),
    ADMIN(Authority.ADMIN);

    private final String authority;

    UserRoleEnum(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return this.authority;
    }

    public static class Authority {
        public static final String USER = "ROLE_USER";
        public static final String ADMIN = "ROLE_ADMIN";
    }

}
