package com.part3_team4.deokhoogam.global.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.DELETE, "/api/users/*/hard").denyAll()
            .anyRequest().permitAll()
        )
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable());

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    configuration.setAllowedOriginPatterns(List.of(
        "http://localhost:3000",
        "https://*.deokhoogam.com" // 배포된 프론트엔드 도메인
    ));
    configuration.addAllowedMethod("*");
    configuration.addAllowedHeader("*");
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  // 프론트엔드 '//' 경로 에러 방어를 위해 DefaultHttpFirewall 유지
  @Bean
  public HttpFirewall defaultHttpFirewall() {
    return new DefaultHttpFirewall();
  }

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return (web) -> web
        .httpFirewall(defaultHttpFirewall())
        .ignoring()
        .requestMatchers(
            "/favicon.ico", "/error",
            "/css/**", "/js/**", "/images/**", "/assets/**"
        );
  }
}