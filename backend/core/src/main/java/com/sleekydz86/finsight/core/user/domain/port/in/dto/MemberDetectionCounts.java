package com.sleekydz86.finsight.core.user.domain.port.in.dto;

import java.util.ArrayList;
import java.util.List;

public record MemberDetectionCounts(long usernameCount, long nicknameCount, long emailCount) {

    public List<String> filledLabels() {
        List<String> labels = new ArrayList<>();
        if (usernameCount > 0) {
            labels.add("아이디");
        }
        if (nicknameCount > 0) {
            labels.add("닉네임");
        }
        if (emailCount > 0) {
            labels.add("이메일");
        }
        return List.copyOf(labels);
    }
}
