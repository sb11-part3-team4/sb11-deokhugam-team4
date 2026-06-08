package com.part3_team4.deokhoogam.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class QuerydslTestConfig {
  @Bean
  public JPAQueryFactory jpaQueryFactory(EntityManager em) {
    return new JPAQueryFactory(em);
  }
}
