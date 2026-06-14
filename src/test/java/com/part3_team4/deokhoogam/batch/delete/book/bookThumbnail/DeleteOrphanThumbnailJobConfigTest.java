package com.part3_team4.deokhoogam.batch.delete.book.bookThumbnail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.part3_team4.deokhoogam.domain.book.entity.OrphanThumbnail;
import com.part3_team4.deokhoogam.global.storage.FileUploader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ItemProcessor;

@ExtendWith(MockitoExtension.class)
class DeleteOrphanThumbnailJobConfigTest {

  @Mock
  private FileUploader fileUploader;

  @InjectMocks
  private DeleteOrphanThumbnailJobConfig deleteOrphanThumbnailJobConfig;

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
}