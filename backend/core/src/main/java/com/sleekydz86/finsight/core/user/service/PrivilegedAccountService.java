package com.sleekydz86.finsight.core.user.service;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.port.out.UserPersistencePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PrivilegedAccountService {

    private static final Logger log = LoggerFactory.getLogger(PrivilegedAccountService.class);

    private final UserPersistencePort userPersistencePort;
    private final Set<String> adminEmails;
    private final Set<String> adminUsernames;

    public PrivilegedAccountService(
            UserPersistencePort userPersistencePort,
            @Value("${finsight.auth.admin-emails:}") String adminEmailsCsv,
            @Value("${finsight.auth.admin-usernames:}") String adminUsernamesCsv) {
        this.userPersistencePort = userPersistencePort;
        this.adminEmails = parseCsv(adminEmailsCsv);
        this.adminUsernames = parseCsv(adminUsernamesCsv);
    }

    @Transactional
    public User ensurePrivilegedRole(User user) {
        if (user == null || (adminEmails.isEmpty() && adminUsernames.isEmpty())) {
            return user;
        }
        if (!isConfiguredAdmin(user)) {
            return user;
        }
        if (user.getRole() == UserRole.ADMIN) {
            return user;
        }
        user.changeRole(UserRole.ADMIN);
        User saved = userPersistencePort.save(user);
        log.info("관리자 계정으로 권한이 승격되었습니다: email={}, username={}",
                saved.getEmail(), saved.getUsername());
        return saved;
    }

    private boolean isConfiguredAdmin(User user) {
        if (StringUtils.hasText(user.getEmail())
                && adminEmails.contains(user.getEmail().trim().toLowerCase(Locale.ROOT))) {
            return true;
        }
        return StringUtils.hasText(user.getUsername())
                && adminUsernames.contains(user.getUsername().trim().toLowerCase(Locale.ROOT));
    }

    private static Set<String> parseCsv(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }
}
