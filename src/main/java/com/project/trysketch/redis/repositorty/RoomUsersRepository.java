package com.project.trysketch.redis.repositorty;

import com.project.trysketch.redis.entity.RoomUsers;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

// 1. 기능   : 비회원 Repository
// 2. 작성자 : 서혁수
public interface RoomUsersRepository extends CrudRepository<RoomUsers, Long> {
    List<RoomUsers> findRoomUsersByRoomNum(Long num);
}
