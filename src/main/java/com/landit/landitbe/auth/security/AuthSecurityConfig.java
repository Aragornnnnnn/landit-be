// 인증 API와 문서, 상태 확인 경로를 허용하는 Spring Security 설정을 정의한다.
package com.landit.landitbe.auth.security;

import com.landit.landitbe.common.exception.ErrorCode;
import com.landit.landitbe.common.web.CorsProperties;
import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class AuthSecurityConfig {

    private static final List<String> CORS_ALLOWED_METHODS = List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
    );
    private static final List<String> CORS_ALLOWED_HEADERS = List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin"
    );
    private static final boolean CORS_ALLOW_CREDENTIALS = true;

    private final AuthTokenFilter authTokenFilter;
    private final AuthFailureResponseWriter failureResponseWriter;

    public AuthSecurityConfig(
            AuthTokenFilter authTokenFilter,
            AuthFailureResponseWriter failureResponseWriter
    ) {
        this.authTokenFilter = authTokenFilter;
        this.failureResponseWriter = failureResponseWriter;
    }

    /** 소셜 로그인은 비인증 상태에서 호출할 수 있도록 허용한다. */
    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
                .authorizeHttpRequests(registry -> registry
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/expressions/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/expressions/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/scenarios").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/scenarios/*/sessions").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/v1/sessions/*/messages").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/sessions/*/end").authenticated()
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/swagger-ui/**", "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(CORS_ALLOWED_METHODS);
        configuration.setAllowedHeaders(CORS_ALLOWED_HEADERS);
        configuration.setAllowCredentials(CORS_ALLOW_CREDENTIALS);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> failureResponseWriter.write(response, ErrorCode.INVALID_TOKEN);
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> failureResponseWriter.write(response, ErrorCode.FORBIDDEN);
    }
}
