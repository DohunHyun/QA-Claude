package com.nh.qagpt.dto;

import com.nh.qagpt.domain.ReviewResult;

public record ReviewResponse(Long reviewId, Integer round, String status, boolean passed, int defectCount) {

    /**
     * defectCount는 호출측에서 count 쿼리로 조회해 넘긴다.
     * (LAZY 컬렉션 r.getDefects()를 세션 밖에서 만지면 LazyInitializationException — open-in-view=false)
     */
    public static ReviewResponse from(ReviewResult r, int defectCount) {
        return new ReviewResponse(
                r.getId(), r.getRound(),
                r.getStatus() == null ? null : r.getStatus().name(),
                r.isPassed(),
                defectCount);
    }
}
