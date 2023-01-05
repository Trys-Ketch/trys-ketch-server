package com.project.trysketch.global.security;

import com.project.trysketch.user.entity.User;
import com.project.trysketch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// 1. 기능   : DB 에서 정보 가져오기
// 2. 작성자 : 서혁수
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new IllegalArgumentException("없는 유저"));

        return new UserDetailsImpl(user, user.getNickname());
    }
}