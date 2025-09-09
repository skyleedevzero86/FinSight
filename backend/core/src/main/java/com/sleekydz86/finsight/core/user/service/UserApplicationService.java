package com.sleekydz86.finsight.core.user.service;

import com.sleekydz86.finsight.core.user.domain.User;
import com.sleekydz86.finsight.core.user.domain.UserRole;
import com.sleekydz86.finsight.core.user.domain.UserStatus;
import com.sleekydz86.finsight.core.user.domain.port.in.dto.*;
import com.sleekydz86.finsight.core.user.domain.port.out.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserApplicationService {

    UserResponse join(UserJoinRequest request);

    UserLoginResponse login(UserLoginRequest request, String clientIp);

    UserResponse getCurrentUserInfo(Long userId);

    UserResponse getUserProfile(Long userId);

    UserResponse getUserInfo(Long userId);

    Page<UserResponse> getUsers(Pageable pageable);

    Page<UserResponse> getPendingUsers(Pageable pageable);

    UserResponse approveUser(Long userId, Long approverId);

    UserResponse rejectUser(Long userId);

    UserResponse suspendUser(Long userId);

    UserResponse unlockUser(Long userId);

    UserResponse updateProfile(Long userId, UserUpdateRequest request);

    UserResponse changeRole(Long userId, UserRole role);

    void deleteUser(Long userId);

    List<UserResponse> getAllUsers();

    Page<UserResponse> getUsersByStatusAndRole(String status, String role, Pageable pageable);

    PasswordStatusResponse getPasswordStatus(Long userId);

    UserResponse resetToPending(Long userId);

    UserResponse changeUserRole(Long userId, UserRole newRole, Long approverId);

    void evictAllUserCache();

    long countUsersByStatus(UserStatus status);

    boolean isPasswordChangeRequired(Long userId);

    boolean isPasswordChangeRecommended(Long userId);

    long getTodayPasswordChangeCount(Long userId);

    Long getUserIdByUsername(String username);

    void changePassword(Long userId, UserPasswordChangeRequest request);
}
