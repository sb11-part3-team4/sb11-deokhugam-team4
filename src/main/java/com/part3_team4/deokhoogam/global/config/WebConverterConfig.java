package com.part3_team4.deokhoogam.global.config;

import com.part3_team4.deokhoogam.domain.book.entity.Direction;
import com.part3_team4.deokhoogam.domain.book.entity.SortType;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConverterConfig implements WebMvcConfigurer {

  @Override
  public void addFormatters(FormatterRegistry registry) {
    registry.addConverter(String.class, SortType.class, SortType::from);
    registry.addConverter(String.class, Direction.class, Direction::from);
  }
}
