package com.digniche.muntum.keyword.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum KeywordCategory {
    EXPERIENCE("체험/활동"),
    FESTIVAL("축제/활발"),
    AESTHETIC("심미/예술"),
    EDUCATION("교육"),
    IMMERSION("몰입/감상"),
    TRENDY("트렌디/SNS"),
    CALM("차분/사색"),
    SITUATION("상황");

    private final String description;
}
