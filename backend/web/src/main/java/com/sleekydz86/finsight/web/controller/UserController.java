package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.global.exception.InvalidPasswordException;
import com.sleekydz86.finsight.core.global.exception.UserNotFoundException;
import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserUpdateRequest;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.WatchlistUpdateRequest;
import com.sleekydz86.finsight.core.user.service.UserService;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.NotificationType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<User> getUser(@PathVariable Long userId) {
        try {
            Optional<User> user = userService.findById(userId);
            return ResponseEntity.ok(user.get());
        } catch (UserNotFoundException e) {
            // 글로벌 예외 처리기가 처리하므로 여기서는 로깅만
            throw e;
        }
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<User> updateUser(@PathVariable Long userId, @RequestBody UserUpdateRequest request) {
        try {
            User updatedUser = userService.updateUser(userId, request);
            return ResponseEntity.ok(updatedUser);
        } catch (UserNotFoundException e) {
            throw e;
        } catch (InvalidPasswordException e) {
            throw e;
        }
    }

    @PutMapping("/{userId}/watchlist")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> updateWatchlist(@PathVariable Long userId,
                                                @RequestBody WatchlistUpdateRequest request) {
        userService.updateWatchlist(userId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{userId}/notifications")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Void> updateNotificationPreferences(@PathVariable Long userId,
                                                              @RequestBody List<NotificationType> preferences) {
        userService.updateNotificationPreferences(userId, preferences);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/watchlist")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<TargetCategory>> getUserWatchlist(@PathVariable Long userId) {
        List<TargetCategory> watchlist = userService.getUserWatchlist(userId);
        return ResponseEntity.ok(watchlist);
    }

    @GetMapping("/{userId}/notifications")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<NotificationType>> getUserNotificationPreferences(@PathVariable Long userId) {
        List<NotificationType> preferences = userService.getUserNotificationPreferences(userId);
        return ResponseEntity.ok(preferences);
    }
}