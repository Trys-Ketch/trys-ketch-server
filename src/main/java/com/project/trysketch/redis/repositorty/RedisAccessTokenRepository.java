package com.project.trysketch.redis.repositorty;

import com.project.trysketch.redis.dto.NonMember;
import org.springframework.data.repository.CrudRepository;

// 1. 기능   : 비회원 Repository
// 2. 작성자 : 서혁수
public interface RedisAccessTokenRepository extends CrudRepository<NonMember, String> {
    // CrudRepository 를 상속 받아서 JPA 처럼 사용한다.
    // 다만, Indexed 를 추가한 속성만을 추가적으로 findBy 등을 추가가 가능하다.
}
