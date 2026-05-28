package com.part3_team4.deokhoogam.domain.test;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

  @GetMapping("/hello")
  public ResponseEntity<String> HelloWorld() {
    return ResponseEntity.ok("Hello World!");
  }


}
