// 로그인 성공 후 사용할 자체 access token과 refresh token을 생성한다.

package com.landit.landitbe.feature.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.config.auth.TokenProperties;
import com.landit.landitbe.feature.auth.domain.UserProfile;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** 로그인 성공 후 사용할 자체 access token과 refresh token을 생성한다. */
@Component
public class LanditTokenService {

  private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final String ACCESS_TOKEN_TYPE = "access";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {};

  private final TokenProperties properties;
  private final ObjectMapper objectMapper;

  /** 동작을 수행한다. */
  public LanditTokenService(TokenProperties properties) {
    this.properties = properties;
    this.objectMapper = new ObjectMapper();
  }

  /** 사용자 식별자를 담은 access token을 생성한다. */
  public String createAccessToken(UserProfile userProfile) {
    return createToken(userProfile, ACCESS_TOKEN_TYPE, properties.accessExpiresInSeconds());
  }

  /** Access token을 검증하고 사용자 PK를 반환한다. */
  public Long parseAccessToken(String token) {
    try {
      String[] parts = token.split("\\.", -1);
      if (parts.length != 3) {
        throw new ApiException(ErrorCode.INVALID_TOKEN);
      }

      String unsignedToken = parts[0] + "." + parts[1];
      if (!MessageDigest.isEqual(
          sign(unsignedToken).getBytes(StandardCharsets.UTF_8),
          parts[2].getBytes(StandardCharsets.UTF_8))) {
        throw new ApiException(ErrorCode.INVALID_TOKEN);
      }

      Map<String, Object> claims =
          objectMapper.readValue(BASE64_URL_DECODER.decode(parts[1]), CLAIMS_TYPE);
      if (!ACCESS_TOKEN_TYPE.equals(claims.get("type"))) {
        throw new ApiException(ErrorCode.INVALID_TOKEN);
      }
      long expiresAt = number(claims.get("exp"));
      if (Instant.now().getEpochSecond() >= expiresAt) {
        throw new ApiException(ErrorCode.INVALID_TOKEN);
      }
      String subject = text(claims.get("sub"));
      if (subject == null || subject.isBlank()) {
        throw new ApiException(ErrorCode.INVALID_TOKEN);
      }
      return Long.valueOf(subject);
    } catch (ApiException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new ApiException(ErrorCode.INVALID_TOKEN);
    }
  }

  /** 난수 기반 refresh token을 생성한다. */
  public String createRefreshToken() {
    byte[] bytes = new byte[32];
    SECURE_RANDOM.nextBytes(bytes);
    return BASE64_URL_ENCODER.encodeToString(bytes);
  }

  /** Refresh token 원문 저장을 피하기 위해 SHA-256 해시를 생성한다. */
  public String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  private String createToken(UserProfile userProfile, String tokenType, long expiresInSeconds) {
    Instant now = Instant.now();
    Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sub", String.valueOf(userProfile.getId()));
    payload.put("type", tokenType);
    payload.put("jti", UUID.randomUUID().toString());
    payload.put("iat", now.getEpochSecond());
    payload.put("exp", now.plusSeconds(expiresInSeconds).getEpochSecond());

    String unsignedToken = encode(header) + "." + encode(payload);
    return unsignedToken + "." + sign(unsignedToken);
  }

  private String encode(Map<String, Object> value) {
    try {
      return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    } catch (JsonProcessingException exception) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  private String sign(String unsignedToken) {
    if (properties.secret() == null || properties.secret().isBlank()) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    try {
      SecretKeySpec secretKey =
          new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
      Mac mac = Mac.getInstance(HMAC_ALGORITHM);
      mac.init(secretKey);
      return BASE64_URL_ENCODER.encodeToString(
          mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  private long number(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    try {
      return Long.parseLong(text(value));
    } catch (NumberFormatException exception) {
      throw new ApiException(ErrorCode.INVALID_TOKEN);
    }
  }

  private String text(Object value) {
    return value == null ? null : String.valueOf(value);
  }
}
