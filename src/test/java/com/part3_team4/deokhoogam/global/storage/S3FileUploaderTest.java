package com.part3_team4.deokhoogam.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.part3_team4.deokhoogam.global.exception.storage.StorageOperationException;
import java.net.URL;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3FileUploaderTest 단위 테스트")
class S3FileUploaderTest {

  @Mock
  private S3Client s3Client;

  @InjectMocks
  private S3FileUploader s3FileUploader;

  private static final String BUCKET_NAME = "deokhugam-bucket";
  private static final String DOMAIN_PATH = "books";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(s3FileUploader, "bucketName", BUCKET_NAME);
  }

  private MockMultipartFile createFixtureFile(String filename, byte[] content) {
    return new MockMultipartFile("thumbnailImage", filename, "image/png", content);
  }

  @Test
  @DisplayName("정상적인 파일이 주어지면 S3Client 업로드 후 URL을 반환한다")
  void upload_ValidFile_ReturnsUrl() throws Exception {
    // given
    MockMultipartFile file = createFixtureFile("test.png", "bytes".getBytes());
    String expectedUrl = "https://deokhoogam-bucket.s3.ap-northeast-2.amazonaws.com/books/dynamic-key.png";

    given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .willReturn(PutObjectResponse.builder().build());

    S3Utilities mockUtilities = mock(S3Utilities.class);
    given(s3Client.utilities()).willReturn(mockUtilities);
    given(mockUtilities.getUrl(any(Consumer.class))).willReturn(new URL(expectedUrl));

    // when
    String resultUrl = s3FileUploader.upload(file, DOMAIN_PATH);

    // then
    assertThat(resultUrl).isEqualTo(expectedUrl);
    then(s3Client).should(times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }


  @Test
  @DisplayName("S3 전송 중 예외 발생 시 cleanup을 수행하고 StorageOperationException을 던진다")
  void upload_S3Exception_TriggersCleanupAndThrowsException() throws Exception {
    // given
    MockMultipartFile file = createFixtureFile("test.png", "bytes".getBytes());
    given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .willThrow(S3Exception.builder().message("S3 장애").build());

    // when & then
    assertThatThrownBy(() -> s3FileUploader.upload(file, DOMAIN_PATH))
        .isInstanceOf(StorageOperationException.class);

    then(s3Client).should(times(1)).deleteObject(any(Consumer.class));
  }

  @Test
  @DisplayName("파일 객체 자체가 null이면 IllegalArgumentException을 던진다")
  void upload_NullFile_ThrowsIllegalArgumentException() {
    assertThatThrownBy(() -> s3FileUploader.upload(null, DOMAIN_PATH))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("파일이 존재하지 않습니다.");
  }

  @Test
  @DisplayName("파일 이름이 없거나 공백이면 IllegalArgumentException을 던진다")
  void upload_EmptyFilename_ThrowsIllegalArgumentException() {
    MockMultipartFile emptyNameFile = createFixtureFile("", "bytes".getBytes());

    assertThatThrownBy(() -> s3FileUploader.upload(emptyNameFile, DOMAIN_PATH))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("파일 이름이 존재하지 않습니다.");
  }

  @Test
  @DisplayName("도메인 경로가 없거나 공백이면 IllegalArgumentException을 던진다")
  void upload_EmptyDomainPath_ThrowsIllegalArgumentException() {
    MockMultipartFile file = createFixtureFile("test.png", "bytes".getBytes());

    assertThatThrownBy(() -> s3FileUploader.upload(file, "   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("도메인 경로가 존재하지 않습니다.");
  }

  @Test
  @DisplayName("허용되지 않는 확장자인 경우 IllegalArgumentException을 던진다")
  void upload_InvalidExtension_ThrowsIllegalArgumentException() {
    MockMultipartFile txtFile = createFixtureFile("invalid.txt", "bytes".getBytes());
    MockMultipartFile badDotFile = createFixtureFile("invalid_file.", "bytes".getBytes());

    assertThatThrownBy(() -> s3FileUploader.upload(txtFile, DOMAIN_PATH))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("지원하지 않는 파일 확장자입니다.");

    assertThatThrownBy(() -> s3FileUploader.upload(badDotFile, DOMAIN_PATH))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("지원하지 않는 파일 확장자입니다.");
  }

  @Test
  @DisplayName("파일 크기가 0바이트인 경우 IllegalArgumentException을 던진다")
  void upload_EmptyFileContent_ThrowsIllegalArgumentException() {
    MockMultipartFile emptyContentFile = createFixtureFile("empty.png", new byte[0]);

    assertThatThrownBy(() -> s3FileUploader.upload(emptyContentFile, DOMAIN_PATH))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("비어있는 파일은 업로드할 수 없습니다.");
  }
}