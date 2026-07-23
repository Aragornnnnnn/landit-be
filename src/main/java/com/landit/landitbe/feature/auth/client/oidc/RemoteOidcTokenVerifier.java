// 실제 OIDC 제공자의 공개키로 ID Token 서명과 claim을 검증한다.

package com.landit.landitbe.feature.auth.client.oidc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.landit.landitbe.config.auth.OidcProperties;
import com.landit.landitbe.feature.auth.domain.SocialProvider;
import com.landit.landitbe.shared.exception.ApiException;
import com.landit.landitbe.shared.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 실제 OIDC 제공자의 공개키로 ID Token 서명과 claim을 검증한다. */
@Component
@ConditionalOnProperty(
    prefix = "landit.auth.oidc",
    name = "fake-enabled",
    havingValue = "false",
    matchIfMissing = true)
public class RemoteOidcTokenVerifier implements OidcTokenVerifier {

  private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
  private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {};
  private static final String RS256_ALGORITHM = "RS256";
  private static final long ALLOWED_CLOCK_SKEW_SECONDS = 300;

  private final OidcProperties properties;
  private final ObjectMapper objectMapper;
  private final HttpClient httpClient;
  private final Map<SocialProvider, JsonNode> jwksCache = new ConcurrentHashMap<>();

  /** 동작을 수행한다. */
  public RemoteOidcTokenVerifier(OidcProperties properties) {
    this.properties = properties;
    this.objectMapper = new ObjectMapper();
    this.httpClient = HttpClient.newHttpClient();
  }

  /** 원격 JWKS를 사용해 ID Token을 검증하고 사용자 정보를 추출한다. */
  @Override
  public OidcUserInfo verify(SocialProvider provider, String idToken, String nonce) {
    ProviderSettings settings = settings(provider);
    String[] parts = splitToken(idToken);
    Map<String, Object> header = decodeJson(parts[0]);
    Map<String, Object> claims = decodeJson(parts[1]);

    verifyAlgorithm(header);
    verifySignature(parts, header, provider, settings);
    verifyClaims(settings, claims, nonce);

    String sub = text(claims.get("sub"));
    if (sub == null || sub.isBlank()) {
      throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
    }
    String email = text(claims.get("email"));
    String nickname = nickname(settings, claims, sub);
    return new OidcUserInfo(provider, sub, email, nickname);
  }

  private ProviderSettings settings(SocialProvider provider) {
    return switch (provider) {
      case GOOGLE ->
          new ProviderSettings(
              List.of("https://accounts.google.com", "accounts.google.com"),
              URI.create("https://www.googleapis.com/oauth2/v3/certs"),
              properties.googleAudiences(),
              List.of("name"));
      case KAKAO ->
          new ProviderSettings(
              List.of("https://kauth.kakao.com"),
              URI.create("https://kauth.kakao.com/.well-known/jwks.json"),
              properties.kakaoAudiences(),
              List.of("nickname"));
      case APPLE ->
          new ProviderSettings(
              List.of("https://appleid.apple.com"),
              URI.create("https://appleid.apple.com/auth/keys"),
              properties.appleAudiences(),
              List.of("name"));
    };
  }

  private String[] splitToken(String idToken) {
    String[] parts = idToken.split("\\.");
    if (parts.length != 3) {
      throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
    }
    return parts;
  }

  private Map<String, Object> decodeJson(String encoded) {
    try {
      byte[] decoded = BASE64_URL_DECODER.decode(encoded);
      return objectMapper.readValue(decoded, CLAIMS_TYPE);
    } catch (IllegalArgumentException | IOException exception) {
      throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
    }
  }

  private void verifyAlgorithm(Map<String, Object> header) {
    if (!RS256_ALGORITHM.equals(text(header.get("alg")))) {
      throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
    }
  }

  private void verifySignature(
      String[] parts,
      Map<String, Object> header,
      SocialProvider provider,
      ProviderSettings settings) {
    String kid = text(header.get("kid"));
    if (kid == null || kid.isBlank()) {
      throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
    }

    try {
      PublicKey publicKey = findPublicKey(provider, settings, kid);
      Signature signature = Signature.getInstance("SHA256withRSA");
      signature.initVerify(publicKey);
      signature.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
      if (!signature.verify(BASE64_URL_DECODER.decode(parts[2]))) {
        throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
      }
    } catch (ApiException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
    }
  }

  private PublicKey findPublicKey(SocialProvider provider, ProviderSettings settings, String kid) {
    try {
      JsonNode key =
          findKey(
              jwksCache.computeIfAbsent(provider, ignored -> fetchJwks(settings.jwksUri())), kid);
      if (key == null) {
        JsonNode refreshedJwks = fetchJwks(settings.jwksUri());
        jwksCache.put(provider, refreshedJwks);
        key = findKey(refreshedJwks, kid);
      }
      if (key == null) {
        throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
      }
      JsonNode certificates = key.path("x5c");
      if (certificates.isArray() && !certificates.isEmpty()) {
        return certificatePublicKey(certificates.get(0).asText());
      }
      return rsaPublicKey(key.path("n").asText(), key.path("e").asText());
    } catch (ApiException exception) {
      throw exception;
    } catch (Exception exception) {
      throw new ApiException(ErrorCode.OIDC_PROVIDER_UNAVAILABLE);
    }
  }

  private JsonNode findKey(JsonNode jwks, String kid) {
    for (JsonNode key : jwks.path("keys")) {
      if (kid.equals(key.path("kid").asText())) {
        return key;
      }
    }
    return null;
  }

  private JsonNode fetchJwks(URI jwksUri) {
    try {
      HttpRequest request = HttpRequest.newBuilder(jwksUri).GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() < 200 || response.statusCode() >= 300) {
        throw new ApiException(ErrorCode.OIDC_PROVIDER_UNAVAILABLE);
      }
      return objectMapper.readTree(response.body());
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new ApiException(ErrorCode.OIDC_PROVIDER_UNAVAILABLE);
    } catch (IOException exception) {
      throw new ApiException(ErrorCode.OIDC_PROVIDER_UNAVAILABLE);
    }
  }

  private PublicKey certificatePublicKey(String certificate) throws Exception {
    byte[] decoded = Base64.getDecoder().decode(certificate);
    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
    return certificateFactory.generateCertificate(new ByteArrayInputStream(decoded)).getPublicKey();
  }

  private RSAPublicKey rsaPublicKey(String modulus, String exponent) throws Exception {
    BigInteger n = new BigInteger(1, BASE64_URL_DECODER.decode(modulus));
    BigInteger e = new BigInteger(1, BASE64_URL_DECODER.decode(exponent));
    return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e));
  }

  private void verifyClaims(ProviderSettings settings, Map<String, Object> claims, String nonce) {
    if (!settings.issuers().contains(text(claims.get("iss")))) {
      throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
    }

    boolean audienceMatched =
        audienceValues(claims.get("aud")).stream().anyMatch(settings.audiences()::contains);
    if (settings.audiences().isEmpty() || !audienceMatched) {
      throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
    }
    Instant now = Instant.now();
    long expiration = number(claims.get("exp"));
    long issuedAt = number(claims.get("iat"));
    if (expiration <= now.minusSeconds(ALLOWED_CLOCK_SKEW_SECONDS).getEpochSecond()
        || issuedAt > now.plusSeconds(ALLOWED_CLOCK_SKEW_SECONDS).getEpochSecond()) {
      throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
    }

    String tokenNonce = text(claims.get("nonce"));
    if (nonce == null || nonce.isBlank() || tokenNonce == null || tokenNonce.isBlank()) {
      throw new ApiException(ErrorCode.OIDC_NONCE_MISMATCH);
    }

    byte[] expected = tokenNonce.getBytes(StandardCharsets.UTF_8);
    byte[] actual = nonce.getBytes(StandardCharsets.UTF_8);
    if (!MessageDigest.isEqual(expected, actual)) {
      throw new ApiException(ErrorCode.OIDC_NONCE_MISMATCH);
    }
  }

  private List<String> audienceValues(Object aud) {
    if (aud instanceof List<?> values) {
      return values.stream()
          .map(this::text)
          .filter(value -> value != null && !value.isBlank())
          .toList();
    }
    String value = text(aud);
    return value == null || value.isBlank() ? List.of() : List.of(value);
  }

  private String nickname(ProviderSettings settings, Map<String, Object> claims, String fallback) {
    for (String nicknameClaim : settings.nicknameClaims()) {
      String nickname = text(claims.get(nicknameClaim));
      if (nickname != null && !nickname.isBlank()) {
        return nickname;
      }
    }
    return fallback;
  }

  private String text(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private long number(Object value) {
    if (value instanceof Number number) {
      return number.longValue();
    }
    try {
      return Long.parseLong(text(value));
    } catch (NumberFormatException exception) {
      throw new ApiException(ErrorCode.OIDC_TOKEN_INVALID);
    }
  }

  private record ProviderSettings(
      List<String> issuers, URI jwksUri, List<String> audiences, List<String> nicknameClaims) {}
}
