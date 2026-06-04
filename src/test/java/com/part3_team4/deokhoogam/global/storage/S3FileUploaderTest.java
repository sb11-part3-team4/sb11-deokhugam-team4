package com.part3_team4.deokhoogam.global.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

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
        eq(null))).willThrow(new RuntimeException("S3 인프라 장애"));

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
}