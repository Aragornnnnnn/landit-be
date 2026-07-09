// 사용자의 학습 언어 설정(학습 대상 locale, 모국어 locale)을 표현한다.
package com.landit.landitbe.auth.application;

public record UserLocale(
        String targetLocale,
        String baseLocale
) {
}
