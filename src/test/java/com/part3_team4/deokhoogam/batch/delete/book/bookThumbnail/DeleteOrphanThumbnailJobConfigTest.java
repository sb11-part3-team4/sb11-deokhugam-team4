package com.part3_team4.deokhoogam.batch.delete.book.bookThumbnail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.part3_team4.deokhoogam.domain.book.entity.OrphanThumbnail;
import com.part3_team4.deokhoogam.domain.book.repository.OrphanThumbnailRepository;
import com.part3_team4.deokhoogam.global.storage.FileUploader;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;

@ExtendWith(MockitoExtension.class)
class DeleteOrphanThumbnailJobConfigTest {

  @Mock
  private FileUploader fileUploader;

  @InjectMocks
  private DeleteOrphanThumbnailJobConfig deleteOrphanThumbnailJobConfig;

  @Mock
  private OrphanThumbnailRepository orphanThumbnailRepository;

  @Mock
  private EntityManagerFactory entityManagerFactory;

  @Mock
  private java.time.Clock clock;

  @Test
  @DisplayName("processor는 고아 썸네일의 파일 URL로 delete()를 호출한다")
  void orphanThumbnailProcessor_deletesFile() throws Exception {
    // given
    OrphanThumbnail orphanThumbnail = new OrphanThumbnail("https://s3.test/orphan.png");
    ItemProcessor<OrphanThumbnail, OrphanThumbnail> processor =
        deleteOrphanThumbnailJobConfig.orphanThumbnailProcessor();

    // when
    OrphanThumbnail result = processor.process(orphanThumbnail);

    // then
    then(fileUploader).should(times(1)).delete("https://s3.test/orphan.png");
    assertThat(result).isEqualTo(orphanThumbnail);
  }

  @Test
  @DisplayName("Reader 및 Writer 빈 생성과 람다 로직이 정상 동작한다")
  void beanCreationAndWriterLogic_success() throws Exception {
    // given
    given(clock.instant()).willReturn(Instant.parse("2026-06-15T10:00:00Z"));

    // when
    deleteOrphanThumbnailJobConfig.orphanThumbnailReader();
    ItemWriter<OrphanThumbnail> writer = deleteOrphanThumbnailJobConfig.orphanThumbnailWriter();
    Chunk<OrphanThumbnail> chunk = new Chunk<>(List.of(new OrphanThumbnail("dummy.png")));

    writer.write(chunk);

    // then
    then(orphanThumbnailRepository).should(times(1)).deleteAllInBatch(any());
  }
}