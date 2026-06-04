package com.part3_team4.deokhoogam.global.config;

import com.part3_team4.deokhoogam.global.jwt.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

@Configuration
public class SecurityConfig {

  private final JwtFilter jwtFilter;

  public SecurityConfig(JwtFilter jwtFilter) {
    this.jwtFilter = jwtFilter;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(session
            -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // 회원가입, 로그인은 토큰 없이 접근 가능
            .requestMatchers(HttpMethod.POST, "/api/users").permitAll()
            .requestMatchers("/api/users/login").permitAll()
            // 메인 페이지 HTML 접근 허용
            .requestMatchers("/", "/index.html").permitAll()
            .anyRequest().authenticated()
        )
        .formLogin(form -> form.disable())
        .httpBasic(basic -> basic.disable());

    http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public HttpFirewall httpFirewall() {
    StrictHttpFirewall firewall = new StrictHttpFirewall();
    firewall.setAllowUrlEncodedDoubleSlash(true);
    firewall.setAllowSemicolon(true);
    firewall.setAllowBackSlash(true);
    return  firewall;
  }

  @Bean
  public WebSecurityCustomizer webSecurityCustomizer(HttpFirewall httpFirewall) {
    return (web) -> web
        .httpFirewall(httpFirewall)
        .ignoring()
        .requestMatchers(
            "/favicon.ico", "/error",
            "/css/**", "/js/**", "/images/**", "/assets/**",
            "/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**"
        );
  }
}