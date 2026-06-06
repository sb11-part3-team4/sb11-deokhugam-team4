package com.part3_team4.deokhoogam.global.util;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateDeserializer extends JsonDeserializer<LocalDate> {
  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  @Override
  public LocalDate deserialize(JsonParser p, DeserializationContext context) throws IOException {
    String date = p.getText();
    if (date == null || date.isBlank()) {
      return null;
    }
    return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
  }
}
