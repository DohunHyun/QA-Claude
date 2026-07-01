package com.nh.qagpt.dto;

import com.nh.qagpt.domain.ReviewResult;

public record ReviewResponse(Long reviewId, Integer round, String status, boolean passed, int defectCount) {

    public static ReviewResponse from(ReviewResult r) {
        return new ReviewResponse(
                r.getId(), r.getRound(),
                r.getStatus() == null ? null : r.getStatus().name(),
                r.isPassed(),
                r.getDefects() == null ? 0 : r.getDefects().size());
    }
}
