package com.sleekydz86.finsight.core.auth.service;

import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaRepository;
import com.sleekydz86.finsight.core.user.adapter.persistence.command.UserJpaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserJpaRepository userJpaRepository;

    public CustomUserDetailsService(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("사용자 인증 정보 로드: {}", email);

        UserJpaEntity userEntity = userJpaRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        if (!userEntity.isActive()) {
            throw new UsernameNotFoundException("비활성화된 사용자입니다: " + email);
        }

        return User.builder()
                .username(userEntity.getEmail())
                .password(userEntity.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + userEntity.getRole().name())))
                .disabled(!userEntity.isActive())
                .build();
    }
}