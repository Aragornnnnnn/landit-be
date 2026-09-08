// 별도 읽기 전용 PostgreSQL 연결에서 관리자 대상 SQL을 제한된 시간과 행 수로 실행한다.

package com.landit.landitbe.feature.notification.service;

import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** 애플리케이션 쓰기 계정으로 대체하지 않는 관리자 대상 조회 서비스다. */
@Service
public class AdminPushAudienceSqlService {

  private static final Pattern LITERAL = Pattern.compile("'(?:[^']|'')*'");
  private static final Pattern FUNCTION = Pattern.compile("([a-z_][a-z0-9_.]*)\\s*\\(");
  private static final Set<String> FUNCTIONS =
      Set.of(
          "exists",
          "in",
          "as",
          "count",
          "min",
          "max",
          "sum",
          "avg",
          "coalesce",
          "nullif",
          "lower",
          "upper",
          "length");
  private static final Pattern FORBIDDEN =
      Pattern.compile(
          "\\b(insert|update|delete|merge|into|copy|call|do|set|reset|alter|drop|"
              + "create|grant|revoke|"
              + "execute|prepare|vacuum|analyze|lock|for|union)\\b");
  private final String url;
  private final String username;
  private final String password;
  private final int maxRows;

  /**
   * 별도 읽기 계정의 연결 설정을 받는다.
   *
   * @param url 읽기 계정용 BE PostgreSQL JDBC URL
   * @param username 읽기 전용 사용자
   * @param password 읽기 계정 비밀번호
   * @param maxRows 조회 결과 상한. 초과 시 전체 조회를 거부
   */
  public AdminPushAudienceSqlService(
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
   * 단일 SELECT 또는 읽기 CTE만 허용한다. 실제 쓰기 차단은 DB 읽기 트랜잭션이 담당한다.
   *
   * @param sql 대상 SQL
   * @throws ApiException 지원하지 않는 SQL 문법일 때 발생
   */
  public static void validateSql(String sql) {
    if (sql == null
        || sql.isBlank()
        || sql.length() > 20000
        || sql.contains("\\")
        || sql.contains("$")
        || sql.contains("\u0000")) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
    String tokens = LITERAL.matcher(sql).replaceAll("''").toLowerCase(Locale.ROOT).strip();
    if (!(tokens.startsWith("select ")
            || tokens.startsWith("select\n")
            || tokens.startsWith("with ")
            || tokens.startsWith("with\n"))
        || tokens.contains(";")
        || tokens.contains("--")
        || tokens.contains("/*")
        || tokens.contains("\\")
        || tokens.contains("$")
        || tokens.contains("\"")
        || FORBIDDEN.matcher(tokens).find()) {
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
    var functions = FUNCTION.matcher(tokens);
    while (functions.find()) {
      if (!FUNCTIONS.contains(functions.group(1))) {
        throw new ApiException(ErrorCode.INVALID_REQUEST);
      }
    }
  }

  /**
   * 양수 정수 user_profile_id 한 컬럼을 반환하는 SQL을 실행한다.
   *
   * @param sql 대상 조회 SQL
   * @return 중복을 제거한 사용자 ID
   * @throws ApiException 설정 누락, 실행 실패, 잘못된 결과 또는 결과 상한 초과 시 발생
   */
  public List<Long> query(String sql) {
    validateSql(sql);
    if (!url.startsWith("jdbc:postgresql:")
        || username.isBlank()
        || password.isBlank()
        || maxRows < 1
        || maxRows == Integer.MAX_VALUE) {
      throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE);
    }
    Properties credentials = new Properties();
    credentials.setProperty("user", username);
    credentials.setProperty("password", password);
    credentials.setProperty("connectTimeout", "5");
    credentials.setProperty("socketTimeout", "15");
    credentials.setProperty("readOnlyMode", "transaction");
    try (Connection connection = DriverManager.getConnection(url, credentials)) {
      connection.setReadOnly(true);
      connection.setAutoCommit(false);
      try {
        configure(connection);
        return readIds(connection, sql);
      } finally {
        connection.rollback();
      }
    } catch (SQLException | ArithmeticException exception) {
      // SQL과 드라이버 예외에는 개인정보가 포함될 수 있으므로 응답·로그에 전달하지 않는다.
      throw new ApiException(ErrorCode.INVALID_REQUEST);
    }
  }

  private void configure(Connection connection) throws SQLException {
    try (var statement = connection.createStatement()) {
      statement.setQueryTimeout(10);
      statement.execute("set local statement_timeout='10s'");
      statement.execute("set local lock_timeout='1s'");
      statement.execute("set local search_path=pg_catalog,public");
      try (var role =
          statement.executeQuery(
              "select rolsuper or rolcreatedb or rolcreaterole or rolreplication or rolbypassrls "
                  + "from pg_roles where rolname=current_user")) {
        if (!role.next() || role.getBoolean(1)) {
          throw new ApiException(ErrorCode.SERVICE_UNAVAILABLE);
        }
      }
    }
  }

  private List<Long> readIds(Connection connection, String sql) throws SQLException {
    try (var statement =
        connection.prepareStatement("select * from (\n" + sql + "\n) audience_result")) {
      statement.setQueryTimeout(10);
      statement.setMaxRows(maxRows + 1);
      // 데이터 전송 전에 형식을 확인해 JSON·TEXT 대량 결과를 메모리에 받지 않는다.
      var metadata = statement.getMetaData();
      if (metadata == null
          || metadata.getColumnCount() != 1
          || !"user_profile_id".equalsIgnoreCase(metadata.getColumnLabel(1))
          || !Set.of(java.sql.Types.BIGINT, java.sql.Types.INTEGER, java.sql.Types.SMALLINT)
              .contains(metadata.getColumnType(1))) {
        throw new ApiException(ErrorCode.INVALID_REQUEST);
      }
      try (var result = statement.executeQuery()) {
        if (result.getMetaData().getColumnCount() != 1
            || !"user_profile_id".equalsIgnoreCase(result.getMetaData().getColumnLabel(1))) {
          throw new ApiException(ErrorCode.INVALID_REQUEST);
        }
        TreeSet<Long> ids = new TreeSet<>();
        int rows = 0;
        while (result.next()) {
          var value = result.getBigDecimal(1);
          if (++rows > maxRows || value == null || value.signum() <= 0) {
            throw new ApiException(ErrorCode.INVALID_REQUEST);
          }
          ids.add(value.longValueExact());
        }
        return List.copyOf(ids);
      }
    }
  }
}
