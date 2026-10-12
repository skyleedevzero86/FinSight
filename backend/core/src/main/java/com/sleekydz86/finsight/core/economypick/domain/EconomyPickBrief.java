package com.sleekydz86.finsight.core.economypick.domain;

import java.util.List;

public record EconomyPickBrief(String source, String headline, String body, List<String> points) {
}
