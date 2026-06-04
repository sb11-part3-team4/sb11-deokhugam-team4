package com.part3_team4.deokhoogam;

import io.awspring.cloud.s3.S3Template;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class DeokhugamApplicationTests {

  @MockitoBean
  private S3Template s3Template;

  @Test
  void contextLoads() {
  }

}
