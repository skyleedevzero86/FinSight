package com.sleekydz86.finsight.core.user.domain.port.in.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 가입 요청 DTO", example = """
        {
          "username": "newuser123",
          "password": "password123",
          "nickname": "새로운사용자",
          "email": "newuser@example.com"
        }
        """)
public class UserJoinRequest {

    @Schema(description = "사용자명 (로그인 ID)", example = "newuser123", minLength = 4, maxLength = 20, pattern = "^[a-zA-Z0-9_]+$", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "사용자명은 필수입니다.")
    @Size(min = 4, max = 20, message = "사용자명은 4~20자 사이여야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "사용자명은 영문, 숫자, 언더스코어만 가능합니다.")
    private String username;

    @Schema(description = "비밀번호 (영문 + 숫자 조합 필수)", example = "password123", minLength = 6, maxLength = 50, pattern = "^(?=.*[a-zA-Z])(?=.*\\d)[A-Za-z\\d@$!%*?&]+$", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 6, max = 50, message = "비밀번호는 6~50자 사이여야 합니다.")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)[A-Za-z\\d@$!%*?&]+$", message = "비밀번호는 영문과 숫자를 포함해야 합니다.")
    private String password;

    @Schema(description = "닉네임 (화면 표시용)", example = "새로운사용자", minLength = 2, maxLength = 20, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2~20자 사이여야 합니다.")
    private String nickname;

    @Schema(description = "이메일 주소 (중복 불가)", example = "newuser@example.com", format = "email", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;
}