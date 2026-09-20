// 알림 이메일의 이미지 배너와 HTML 본문을 구성한다.

package com.landit.landitbe.feature.notification.email.service;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/** 텍스트 안내를 이스케이프해 이미지와 함께 이메일 본문으로 구성한다. */
@Service
public class NotificationEmailTemplateService {
  /** 발신 전용 이메일 공통 안내이다. */
  public static final String FOOTER = "이 메일은 발신 전용이며 답장을 확인하지 않습니다.";

  /**
   * 체험 안내 또는 관리자 테스트용 HTML을 생성한다.
   *
   * @param subject 이메일 제목
   * @param message 일반 텍스트 안내
   * @param managementUrl 서버에서 선택한 스토어 HTTPS 주소 또는 버튼이 없으면 null
   * @return 인라인 이미지와 텍스트 안내를 포함한 HTML
   */
  public String render(String subject, String message, String managementUrl) {
    String button = managementUrl == null ? "" : subscriptionButton(managementUrl);
    String template =
        """
        <!doctype html>
        <html lang="ko">
        <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"></head>
        <body style="margin:0;padding:24px 12px;background-color:#f5f5f5;font-family:Arial,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;color:#222222;">
          <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
            <tr><td align="center">
              <table role="presentation" width="560" cellspacing="0" cellpadding="0" border="0" style="width:100%%;max-width:560px;background-color:#ffffff;">
                <tr><td><img src="cid:landit-banner" alt="Landit" width="560" style="display:block;width:100%%;max-width:560px;height:auto;border:0;"></td></tr>
                <tr><td style="padding:32px 24px;">
                  <h1 style="margin:0 0 20px;font-size:22px;line-height:1.5;">%s</h1>
                  <p style="margin:0;font-size:16px;line-height:1.8;word-break:keep-all;">%s</p>
                  %s
                  <p style="margin:28px 0 0;padding-top:20px;border-top:1px solid #eeeeee;font-size:12px;line-height:1.6;color:#777777;">%s</p>
                </td></tr>
              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """;
    return template.formatted(
        HtmlUtils.htmlEscape(subject),
        HtmlUtils.htmlEscape(message).replace("\n", "<br>"),
        button,
        FOOTER);
  }

  private String subscriptionButton(String url) {
    String template =
        """
        <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin-top:24px;">
          <tr><td bgcolor="#ff8b12" style="border-radius:8px;text-align:center;">
            <a href="%s" style="display:inline-block;padding:14px 24px;color:#222222;font-size:16px;font-weight:bold;text-decoration:none;">구독 관리하기</a>
          </td></tr>
        </table>
        """;
    return template.formatted(HtmlUtils.htmlEscape(url));
  }
}
