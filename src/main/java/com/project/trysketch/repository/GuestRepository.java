package com.project.trysketch.repository;

import com.project.trysketch.dto.CacheKey;
import com.project.trysketch.entity.Guest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.repository.CrudRepository;
import java.util.Optional;

// 1. 기능   : 비회원 Repository
// 2. 작성자 : 서혁수
public interface GuestRepository extends CrudRepository<Guest, String> {
    // CrudRepository 를 상속 받아서 JPA 처럼 사용한다.
    // 다만, Indexed 를 추가한 속성만을 추가적으로 findBy 등을 추가가 가능하다.
    @Cacheable(value = CacheKey.USER, key = "#id", cacheManager = "CacheManager", unless = "#result == null")
    Optional<Guest> findByGuestId(String id);
}
