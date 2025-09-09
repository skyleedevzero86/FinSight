package com.sleekydz86.finsight.core.auth.service;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    private final UserPersistencePort userPersistencePort;

    public CustomUserDetailsService(UserPersistencePort userPersistencePort) {
        this.userPersistencePort = userPersistencePort;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("사용자 인증 정보 로드: {}", email);

        User user = userPersistencePort.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        if (!user.isActive()) {
            throw new UsernameNotFoundException("비활성화된 사용자입니다: " + email);
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .disabled(!user.isActive())
                .build();
    }
}