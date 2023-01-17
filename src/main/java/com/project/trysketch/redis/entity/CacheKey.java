package com.project.trysketch.redis.entity;

public class CacheKey {
    private CacheKey() {
    }
    public static final int DEFAULT_EXPIRE_SEC = 18000;
    public static final String USER = "user";
    public static final int USER_EXPIRE_SEC = 18000;
}
