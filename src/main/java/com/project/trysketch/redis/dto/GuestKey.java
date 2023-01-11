package com.project.trysketch.redis.dto;

public enum GuestKey {
    GUEST_NUM("guest"),
    GUEST_NICK("nickname")
    ;

    private final String key;   // Enum 요소에 특정 값을 매핑하기 위해서 필드값 추가

    // Enum 클래스 특성으로 인해 생성자가 있어도 new 연산으로 생성이 불가능하다.
    // 필드값 추가시 주의할 점 : Enum 클래스 자체에 name() 메소드가 있기 때문에 name 은 기피할 것
    GuestKey(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
