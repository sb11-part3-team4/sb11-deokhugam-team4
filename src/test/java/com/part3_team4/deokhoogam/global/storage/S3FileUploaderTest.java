package com.part3_team4.deokhoogam.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import com.part3_team4.deokhoogam.global.exception.ErrorKey;
import com.part3_team4.deokhoogam.global.exception.storage.InvalidFileException;
import com.part3_team4.deokhoogam.global.exception.storage.StorageOperationException;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import java.io.InputStream;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3FileUploaderTest 단위 테스트")
class S3FileUploaderTest {

  @Mock
  private S3Template s3Template;

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
  @DisplayName("정상적인 파일이 주어지면 S3 업로드 후 URL을 반환한다")
  void upload_ValidFile_ReturnsUrl() throws Exception {
    // given
    MockMultipartFile file = createFixtureFile("test.png", "bytes".getBytes());
    String expectedUrl = "https://deokhoogam-bucket.s3.ap-northeast-2.amazonaws.com/books/dynamic-key.png";

    S3Resource mockResource = mock(S3Resource.class);

    given(mockResource.getURL()).willReturn(new URL(expectedUrl));

    given(s3Template.upload(eq(BUCKET_NAME), any(String.class), any(InputStream.class),
        eq(null))).willReturn(mockResource);

    // when
    String resultUrl = s3FileUploader.upload(file, DOMAIN_PATH);

    // then
    assertThat(resultUrl).isEqualTo(expectedUrl);

    then(s3Template).should()
        .upload(eq(BUCKET_NAME), any(String.class), any(InputStream.class), eq(null));
  }

  @Test
  @DisplayName("S3 업로드 중 예외가 발생하면 cleanup 후 StorageOperationException을 던진다")
  void upload_S3Exception_TriggersCleanupAndThrowsException() {
    // given
    MockMultipartFile file = createFixtureFile("test.png", "bytes".getBytes());

    given(s3Template.upload(eq(BUCKET_NAME), any(String.class), any(InputStream.class),
        eq(null))).willThrow(S3Exception.builder().message("S3 인프라 장애").build());

    // when
    Throwable thrown = catchThrowable(() -> s3FileUploader.upload(file, DOMAIN_PATH));

    // then
    assertThat(thrown).isInstanceOf(StorageOperationException.class);
    then(s3Template).should().deleteObject(eq(BUCKET_NAME), any(String.class));
  }

  @Test
  @DisplayName("파일 객체가 null이면 InvalidFileException을 던진다")
  void upload_NullFile_ThrowsInvalidFileException() {
    // when
    Throwable thrown = catchThrowable(() -> s3FileUploader.upload(null, DOMAIN_PATH));

    // then
    assertThat(thrown)
        .isInstanceOf(InvalidFileException.class)
        .satisfies(exception -> {
          InvalidFileException e = (InvalidFileException) exception;

          assertThat(e.getDetails())
              .containsEntry(ErrorKey.FIELD.getValue(), ErrorKey.FILE.getValue())
              .containsEntry(ErrorKey.REASON.getValue(), "파일이 존재하지 않습니다.");
        });

    then(s3Template).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("파일 이름이 공백이면 InvalidFileException을 던진다")
  void upload_EmptyFilename_ThrowsInvalidFileException() {
    // given
    MockMultipartFile emptyNameFile = createFixtureFile("", "bytes".getBytes());

    // when
    Throwable thrown = catchThrowable(() -> s3FileUploader.upload(emptyNameFile, DOMAIN_PATH));

    // then
    assertThat(thrown)
        .isInstanceOf(InvalidFileException.class)
        .satisfies(exception -> {
          InvalidFileException e = (InvalidFileException) exception;

          assertThat(e.getDetails())
              .containsEntry(ErrorKey.FIELD.getValue(), ErrorKey.FILE.getValue())
              .containsEntry(ErrorKey.REASON.getValue(), "파일 이름이 존재하지 않습니다.");
        });

    then(s3Template).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("도메인 경로가 공백이면 InvalidFileException을 던진다")
  void upload_EmptyDomainPath_ThrowsInvalidFileException() {
    // given
    MockMultipartFile file =
        createFixtureFile("test.png", "bytes".getBytes());

    // when
    Throwable thrown = catchThrowable(() -> s3FileUploader.upload(file, "   "));

    // then
    assertThat(thrown)
        .isInstanceOf(InvalidFileException.class)
        .satisfies(exception -> {
          InvalidFileException e = (InvalidFileException) exception;

          assertThat(e.getDetails())
              .containsEntry(ErrorKey.FIELD.getValue(), ErrorKey.DOMAIN_PATH.getValue())
              .containsEntry(ErrorKey.REASON.getValue(), "도메인 경로가 존재하지 않습니다.");
        });

    then(s3Template).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("지원하지 않는 확장자이면 InvalidFileException을 던진다")
  void upload_UnsupportedExtension_ThrowsInvalidFileException() {
    // given
    MockMultipartFile txtFile = createFixtureFile("invalid.txt", "bytes".getBytes());

    // when
    Throwable thrown = catchThrowable(() -> s3FileUploader.upload(txtFile, DOMAIN_PATH));

    // then
    assertThat(thrown)
        .isInstanceOf(InvalidFileException.class)
        .satisfies(exception -> {
          InvalidFileException e = (InvalidFileException) exception;

          assertThat(e.getDetails())
              .containsEntry(ErrorKey.FIELD.getValue(), ErrorKey.FILE.getValue())
              .containsEntry(ErrorKey.VALUE.getValue(), "invalid.txt");
        });

    then(s3Template).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("확장자 형식이 잘못되면 InvalidFileException을 던진다")
  void upload_MalformedExtension_ThrowsInvalidFileException() {
    // given
    MockMultipartFile malformedFile = createFixtureFile("invalid_file.", "bytes".getBytes());

    // when
    Throwable thrown = catchThrowable(() -> s3FileUploader.upload(malformedFile, DOMAIN_PATH));

    // then
    assertThat(thrown)
        .isInstanceOf(InvalidFileException.class)
        .satisfies(exception -> {
          InvalidFileException e = (InvalidFileException) exception;

          assertThat(e.getDetails())
              .containsEntry(ErrorKey.FIELD.getValue(), ErrorKey.FILE.getValue())
              .containsEntry(ErrorKey.VALUE.getValue(), "invalid_file.");
        });

    then(s3Template).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("파일 크기가 0바이트이면 InvalidFileException을 던진다")
  void upload_EmptyFileContent_ThrowsInvalidFileException() {
    // given
    MockMultipartFile emptyContentFile = createFixtureFile("empty.png", new byte[0]);

    // when
    Throwable thrown = catchThrowable(() -> s3FileUploader.upload(emptyContentFile, DOMAIN_PATH));

    // then
    assertThat(thrown)
        .isInstanceOf(InvalidFileException.class)
        .satisfies(exception -> {
          InvalidFileException e = (InvalidFileException) exception;

          assertThat(e.getDetails())
              .containsEntry(ErrorKey.FIELD.getValue(), ErrorKey.FILE.getValue())
              .containsEntry(ErrorKey.REASON.getValue(), "비어있는 파일은 업로드할 수 없습니다.");
        });

    then(s3Template).shouldHaveNoInteractions();
  }

  @Test
  @DisplayName("예상하지 못한 RuntimeException은 그대로 전파한다")
  void upload_UnexpectedRuntimeException_Propagates() {
    // given
    MockMultipartFile file = createFixtureFile("test.png", "bytes".getBytes());

    given(s3Template.upload(eq(BUCKET_NAME), any(String.class), any(InputStream.class),
        eq(null))).willThrow(new IllegalArgumentException("bug"));

    // when & then
    assertThatThrownBy(() -> s3FileUploader.upload(file, DOMAIN_PATH)).isInstanceOf(
        IllegalArgumentException.class);
  }


  @Nested
  @DisplayName("S3 삭제 로직에서")
  class S3_delete_Test {

    @Test
    @DisplayName("정상적인 전체 URL이 주어지면 Object Key를 올바르게 추출하여 S3에서 삭제한다")
    void delete_success() {
      // given
      String fullUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/book/1234_test.png";
      String expectedKey = "book/1234_test.png";

      //when
      s3FileUploader.delete(fullUrl);

      //then
      then(s3Template).should().deleteObject(BUCKET_NAME, expectedKey);
    }

    @Test
    @DisplayName("URL이 null이거나 빈 문자열이면 아무 동작도 하지 않는다")
    void delete_ignores_when_url_is_blank() {
      // given & when
      s3FileUploader.delete(null);
      s3FileUploader.delete("");
      s3FileUploader.delete("   ");

      // then (💡 BDD 스타일 검증)
      then(s3Template).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("java.net.URI가 파싱할 수 없는 이상한 문자열이 들어오면 InvalidFileException이 발생한다")
    void delete_throws_exception_when_url_is_invalid() {
      // given
      String invalidUrl = "htp:// wrong-url.com/book/1234_test.png";

      // when
      assertThatThrownBy(() -> s3FileUploader.delete(invalidUrl))
          .isInstanceOf(InvalidFileException.class);

      // then
      then(s3Template).shouldHaveNoInteractions();
    }


    @Test
    @DisplayName("S3 내부에서 삭제 중 예외가 발생하더라도 로직이 터지지 않고(Soft fail) 정상 종료된다")
    void delete_soft_fail_when_s3_throws_exception() {
      // given
      String fullUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/book/error.png";
      String expectedKey = "book/error.png";

      S3Exception s3Exception = mock(S3Exception.class);

      willThrow(s3Exception).given(s3Template).deleteObject(BUCKET_NAME, expectedKey);

      // when & then
      assertThatCode(() -> s3FileUploader.delete(fullUrl))
          .doesNotThrowAnyException();
    }
  }
}