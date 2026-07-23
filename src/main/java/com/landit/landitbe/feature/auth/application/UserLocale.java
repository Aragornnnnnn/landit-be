// 사용자의 학습 언어 설정(학습 대상 locale, 모국어 locale)을 표현한다.

package com.landit.landitbe.feature.auth.application;

import com.landit.landitbe.shared.domain.Locale;

/** 사용자의 학습 언어 설정(학습 대상 locale, 모국어 locale)을 표현한다. */
public record UserLocale(Locale targetLocale, Locale baseLocale) {}
