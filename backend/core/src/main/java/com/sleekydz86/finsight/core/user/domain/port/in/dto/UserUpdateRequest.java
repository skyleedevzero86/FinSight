package com.sleekydz86.finsight.core.user.domain.port.in.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {

    @Size(min = 3, max = 50, message = "사용자명은 3-50자 사이여야 합니다")
    private String username;

    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    private String password;

    @Size(max = 50, message = "닉네임은 50자를 초과할 수 없습니다")
    private String nickname;
}