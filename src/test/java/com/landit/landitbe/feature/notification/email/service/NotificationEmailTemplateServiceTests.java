// 이메일 HTML의 이스케이프와 구독 관리 버튼 표시 조건을 검증한다.

package com.landit.landitbe.feature.notification.email.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationEmailTemplateServiceTests {
  private final NotificationEmailTemplateService template = new NotificationEmailTemplateService();

  @DisplayName("이메일 문구를 이스케이프하면서 스토어 링크와 배너는 유지한다.")
  @Test
  void escapesTextAndKeepsStoreLinkAndBanner() {
    String html =
        template.render(
            "<title>",
            "<script>alert('x')</script>\n다음 줄",
            "https://apps.apple.com/account/subscriptions?x=1&y=2");
    assertThat(html)
        .contains(
            "&lt;title&gt;",
            "&lt;script&gt;",
            "<br>다음 줄",
            "cid:landit-banner",
            "href=\"https://apps.apple.com/account/subscriptions?x=1&amp;y=2\"",
            "구독 관리하기")
        .doesNotContain("<script>");
  }

  @DisplayName("관리자 테스트 이메일에는 구독 버튼을 빼고 하단 안내를 유지한다.")
  @Test
  void adminTestOmitsSubscriptionButtonButKeepsFooter() {
    assertThat(template.render("테스트", "테스트 본문", null))
        .contains("테스트 본문", NotificationEmailTemplateService.FOOTER, "cid:landit-banner")
        .doesNotContain("href=", "구독 관리하기");
  }
}
