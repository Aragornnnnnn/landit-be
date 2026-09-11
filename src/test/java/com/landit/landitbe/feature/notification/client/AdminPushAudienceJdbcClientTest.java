// 읽기 DB URL 옵션이 TLS와 별도 계정 정책을 우회하지 못하는지 검증한다.

package com.landit.landitbe.feature.notification.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.postgresql.Driver;

class AdminPushAudienceJdbcClientTest {
  @ParameterizedTest
  @ValueSource(strings = {"disable", "allow", "prefer", "require", "verify-ca", "verify-full"})
  void forcesVerifiedTlsAndPreservesDestinationWhenUrlTriesToOverridePolicy(String mode) {
    var client =
        new AdminPushAudienceJdbcClient(
            "jdbc:postgresql://db.example.com:6543/audience?sslmode="
                + mode
                + "&sslfactory=org.postgresql.ssl.NonValidatingFactory&sslhostnameverifier=unsafe"
                + "&user=wrong&password=wrong&connectTimeout=0&socketTimeout=0&readOnlyMode=ignore",
            "reader",
            "fixture",
            100);
    var properties = Driver.parseURL("jdbc:postgresql://", client.connectionProperties());
    assertThat(properties.getProperty("PGHOST")).isEqualTo("db.example.com");
    assertThat(properties.getProperty("PGPORT")).isEqualTo("6543");
    assertThat(properties.getProperty("PGDBNAME")).isEqualTo("audience");
    assertThat(properties.getProperty("sslmode")).isEqualTo("verify-full");
    assertThat(properties.getProperty("sslfactory"))
        .isEqualTo("org.postgresql.ssl.DefaultJavaSSLFactory");
    assertThat(properties).doesNotContainKey("sslhostnameverifier");
    assertThat(properties.getProperty("user")).isEqualTo("reader");
    assertThat(properties.getProperty("password")).isEqualTo("fixture");
    assertThat(properties.getProperty("connectTimeout")).isEqualTo("5");
    assertThat(properties.getProperty("socketTimeout")).isEqualTo("15");
    assertThat(properties.getProperty("readOnlyMode")).isEqualTo("transaction");
  }

  @ParameterizedTest
  @ValueSource(strings = {"localhost", "127.0.0.1", "[::1]"})
  void allowsLocalPostgresFixtureWithoutTls(String host) {
    var client =
        new AdminPushAudienceJdbcClient(
            "jdbc:postgresql://" + host + "/fixture", "reader", "fixture", 100);
    assertThat(client.connectionProperties().getProperty("sslmode")).isEqualTo("disable");
  }

  @Test
  void doesNotTreatLookalikeOrMixedRemoteHostsAsLoopback() {
    for (String host : new String[] {"localhost.example.com", "127.0.0.1,db.example.com"}) {
      var client =
          new AdminPushAudienceJdbcClient(
              "jdbc:postgresql://" + host + "/fixture", "reader", "fixture", 100);
      assertThat(client.connectionProperties().getProperty("sslmode")).isEqualTo("verify-full");
    }
  }
}
