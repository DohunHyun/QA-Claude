package com.nh.qagpt.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

/** 프로젝트 등록 요청. 코드·명·단계별 기간은 명명규칙 검증의 1순위 기준값이 된다. */
public record ProjectRequest(
        @NotBlank String name,
        @NotBlank String code,
        LocalDate managementStart,
        LocalDate managementEnd,
        LocalDate analysisStart,
        LocalDate analysisEnd,
        LocalDate designStart,
        LocalDate designEnd
) {}
