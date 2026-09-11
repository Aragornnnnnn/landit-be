// 관리자 대상 SQL용 읽기 계정 연결과 원격 PostgreSQL TLS 검증을 강제한다.

package com.landit.landitbe.feature.notification.client;

import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import org.postgresql.Driver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 쓰기 DB로 대체하지 않고 JVM 신뢰 저장소로 원격 서버를 검증하는 연결 Adapter다. */
@Component
public class AdminPushAudienceJdbcClient {
  private final String url;
  private final String username;
  private final String password;
  private final int maxRows;

  /**
   * 별도 읽기 계정의 연결 설정을 받는다.
   *
   * @param url 읽기 PostgreSQL JDBC URL
   * @param username 읽기 계정 이름
   * @param password 읽기 계정 비밀번호
   * @param maxRows 중복 제거 전 결과 행 상한
   */
  public AdminPushAudienceJdbcClient(
      @Value("${landit.notification.audience-query.url:}") String url,
      @Value("${landit.notification.audience-query.username:}") String username,
      @Value("${landit.notification.audience-query.password:}") String password,
      @Value("${landit.notification.audience-query.max-rows:100000}") int maxRows) {
    this.url = url;
    this.username = username;
    this.password = password;
    this.maxRows = maxRows;
  }

  /**
   * 제한 시간과 TLS 정책이 적용된 연결을 연다. 호출자가 연결을 닫는다.
   *
   * @return 별도 읽기 계정의 연결
   * @throws ApiException 연결 설정이 누락되거나 잘못된 경우 발생
   * @throws SQLException 인증, TLS 검증 또는 DB 연결 실패 시 발생
   */
  public Connection open() throws SQLException {
    // 원본 URL의 옵션이 보안 정책을 덮어쓰지 않도록 파싱 후 URL 인자 없이 전달한다.
    return DriverManager.getConnection("jdbc:postgresql://", connectionProperties());
  }

  /**
   * 결과 행 상한을 반환한다.
   *
   * @return 중복 제거 전 행 상한
   */
  public int maxRows() {
    return maxRows;
  }

  Properties connectionProperties() {
    Properties properties = url.startsWith("jdbc:postgresql:") ? Driver.parseURL(url, null) : null;
    if (properties == null
        || username.isBlank()
        || password.isBlank()
        || maxRows < 1
        || maxRows == Integer.MAX_VALUE) {
      throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE);
    }
    properties.setProperty("user", username);
    properties.setProperty("password", password);
    properties.setProperty("connectTimeout", "5");
    properties.setProperty("socketTimeout", "15");
    properties.setProperty("readOnlyMode", "transaction");
    boolean loopback =
        Arrays.stream(properties.getProperty("PGHOST").split(","))
            .allMatch(Set.of("localhost", "127.0.0.1", "[::1]")::contains);
    properties.setProperty("sslmode", loopback ? "disable" : "verify-full");
    properties.setProperty("sslfactory", "org.postgresql.ssl.DefaultJavaSSLFactory");
    properties.remove("sslhostnameverifier");
    properties.remove("sslfactoryarg");
    properties.remove("socketFactory");
    properties.remove("socketFactoryArg");
    return properties;
  }
}
