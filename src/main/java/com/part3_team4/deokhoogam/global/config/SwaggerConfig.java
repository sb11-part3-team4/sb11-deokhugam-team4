package com.part3_team4.deokhoogam.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI openAPI() {
    String jwtSchemeName = "jwtAuth";

    // API 요청 시 헤더에 토큰을 넣도록 설정
    SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

    // Swagger UI에 토큰 입력 팝업을 띄우기 위한 설정
    Components components = new Components()
        .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
            .name(jwtSchemeName)
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT"));

    return new OpenAPI()
        .addSecurityItem(securityRequirement)
        .components(components);
  }
}