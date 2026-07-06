// 인증 API와 문서, 상태 확인 경로를 허용하는 Spring Security 설정을 정의한다.
package com.landit.landitbe.auth.security;

import com.landit.landitbe.common.exception.ErrorCode;
import jakarta.servlet.DispatcherType;
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

@Configuration
public class AuthSecurityConfig {

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
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
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
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info", "/swagger-ui/**", "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> failureResponseWriter.write(response, ErrorCode.AUTH_REQUIRED);
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> failureResponseWriter.write(response, ErrorCode.FORBIDDEN);
    }
}
