package com.part3_team4.deokhoogam.batch.listener;

import com.part3_team4.deokhoogam.domain.book.entity.OrphanThumbnail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SkipLoggingListener implements SkipListener<OrphanThumbnail, OrphanThumbnail> {

  @Override
  public void onSkipInProcess(OrphanThumbnail item, Throwable t) {
    log.warn("외부 호출 결과 및 보상 작업 실패 - S3 파일 물리 삭제 스킵 발생. Target URL: {}, 원인: {}",
        item.getFileUrl(), t.getMessage());
  }

  @Override
  public void onSkipInRead(Throwable t) {
    log.warn("배치 Read 스킵 발생 - 원인: {}", t.getMessage());
  }

  @Override
  public void onSkipInWrite(OrphanThumbnail item, Throwable t) {
    log.warn("배치 Write 스킵 발생 - Target URL: {}, 원인: {}", item.getFileUrl(),
        t.getMessage());
  }
}