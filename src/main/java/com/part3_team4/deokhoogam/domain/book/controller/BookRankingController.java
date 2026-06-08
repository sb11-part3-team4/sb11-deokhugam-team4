package com.part3_team4.deokhoogam.domain.ranking.controller;

import com.part3_team4.deokhoogam.domain.ranking.dto.BookRankingDto;
import com.part3_team4.deokhoogam.domain.ranking.dto.RankingGetListRequest;
import com.part3_team4.deokhoogam.domain.ranking.service.BookRankingService;
import com.part3_team4.deokhoogam.global.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books/popular")
@RequiredArgsConstructor
public class BookRankingController {

  private final BookRankingService bookRankingService;

  @GetMapping
  public ResponseEntity<PageResponse<BookRankingDto>> getRankings(
      @Valid @ModelAttribute RankingGetListRequest request) {

    PageResponse<BookRankingDto> response = bookRankingService.getRankings(
        request.period(), request.direction(),request.cursor(), request.limit());

    return ResponseEntity.ok().body(response);
  }
}
