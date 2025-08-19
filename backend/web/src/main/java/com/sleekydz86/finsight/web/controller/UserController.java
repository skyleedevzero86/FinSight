package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.UserUpdateRequest;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.WatchlistUpdateRequest;
import com.sleekydz86.finsight.core.user.service.UserService;
import com.sleekydz86.finsight.core.news.domain.vo.TargetCategory;
import com.sleekydz86.finsight.core.user.domain.NotificationType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable Long userId) {
        return userService.findById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{userId}")
    public ResponseEntity<User> updateUser(@PathVariable Long userId, @RequestBody UserUpdateRequest request) {
        User updatedUser = userService.updateUser(userId, request);
        return ResponseEntity.ok(updatedUser);
    }

    @PutMapping("/{userId}/watchlist")
    public ResponseEntity<Void> updateWatchlist(@PathVariable Long userId,
            @RequestBody WatchlistUpdateRequest request) {
        userService.updateWatchlist(userId, request);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{userId}/notifications")
    public ResponseEntity<Void> updateNotificationPreferences(@PathVariable Long userId,
            @RequestBody List<NotificationType> preferences) {
        userService.updateNotificationPreferences(userId, preferences);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/watchlist")
    public ResponseEntity<List<TargetCategory>> getUserWatchlist(@PathVariable Long userId) {
        List<TargetCategory> watchlist = userService.getUserWatchlist(userId);
        return ResponseEntity.ok(watchlist);
    }

    @GetMapping("/{userId}/notifications")
    public ResponseEntity<List<NotificationType>> getUserNotificationPreferences(@PathVariable Long userId) {
        List<NotificationType> preferences = userService.getUserNotificationPreferences(userId);
        return ResponseEntity.ok(preferences);
    }
}