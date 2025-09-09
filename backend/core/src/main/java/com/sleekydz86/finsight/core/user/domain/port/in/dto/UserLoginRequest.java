package com.sleekydz86.finsight.core.user.domain.port.in.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Builder
@Setter
@Schema(description = "사용자 로그인 요청 DTO", example = """
        {
          "username": "testuser",
          "password": "password123"
        }
        """)
public class UserLoginRequest {

    @Schema(description = "사용자명 (로그인 ID)", example = "testuser", maxLength = 50, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "사용자명은 필수입니다.")
    @Size(max = 50, message = "사용자명은 50자를 초과할 수 없습니다.")
    private String username;

    @Schema(description = "비밀번호", example = "password123", maxLength = 50, format = "password", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(max = 50, message = "비밀번호는 50자를 초과할 수 없습니다.")
    private String password;
}
