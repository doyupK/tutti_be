package com.tutti.backend.controller;

import com.tutti.backend.domain.User;
import com.tutti.backend.dto.Feed.FeedRequestDto;
import com.tutti.backend.dto.Feed.FeedUpdateRequestDto;
import com.tutti.backend.security.UserDetailsImpl;
import com.tutti.backend.security.jwt.HeaderTokenExtractor;
import com.tutti.backend.security.jwt.JwtDecoder;
import com.tutti.backend.service.FeedService;
import com.tutti.backend.service.NotificationService;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

@RestController
@RequiredArgsConstructor
@Slf4j
public class FeedController {
    private final FeedService feedService;
    private final HeaderTokenExtractor headerTokenExtractor;
    private final JwtDecoder jwtDecoder;


    // 메인 페이지
    @GetMapping("/")
    @Timed(value = "Get Main page", description = "Time to Get Main page response")
    public ResponseEntity<?> getMainPage(HttpServletRequest httpServletRequest) {
        String jwtToken = httpServletRequest.getHeader("Authorization");
        if (Objects.equals(jwtToken, "")) {
            return feedService.getMainPage();
        }
        return feedService.getMainPageByUser(
                jwtDecoder.decodeUsername(headerTokenExtractor.extract(jwtToken, httpServletRequest)));
    }




    // 최신 순 전체 피드 따로 가져오기
    @GetMapping("/feeds")
    @Timed(value = "Get All Feed Data", description = "Time to Get All Feed Data")
    public ResponseEntity<?> getFeedPage(
            @RequestParam String postType
            ,@RequestParam(required = false) String genre
            ,@RequestParam int page
            ,@RequestParam int limit){
        page = page - 1;
        Pageable pageable = PageRequest.of(page,limit);
        return feedService.getFeedPage(postType,genre,pageable);
    }
    // 장르 별 피드 따로 가져오기
    /*@GetMapping("/feeds/search")
    public ResponseEntity<?> getFeedByGenrePage(@RequestParam String genre){
        return feedService.getFeedByGenrePage(genre);
    }*/



    // 피드 작성
    @PostMapping("/feeds/upload")
    @Timed(value = "Write feed", description = "Time to write feed")
    public ResponseEntity<?> createFeed(
            @RequestPart FeedRequestDto feedRequestDto,
            @RequestPart MultipartFile albumImage,
            @RequestPart MultipartFile song,
            @AuthenticationPrincipal UserDetailsImpl userDetails){
        User user = userDetails.getUser();
        feedService.createFeed(feedRequestDto,albumImage,song,user);
        return ResponseEntity.ok().body("피드 등록 완료");
    }

    // 피드 수정
    @PutMapping("/feeds/{feedId}")
    @Timed(value = "Modify feed", description = "Time to modify feed")
    public ResponseEntity<?> updateFeed(
            @PathVariable Long feedId,
            @RequestBody FeedUpdateRequestDto feedUpdateRequestDto,
            @AuthenticationPrincipal UserDetailsImpl userDetails
            ){
        User user = userDetails.getUser();
        feedService.updateFeed(feedId,feedUpdateRequestDto,user);
        return ResponseEntity.ok().body("피드 수정 완료");
    }

    // 피드 상세 조회
    @GetMapping("/feeds/{feedId}")
    @Timed(value = "Get feed detail page", description = "Time to Get feed detail page response")
    public ResponseEntity<?> getFeed(
            @PathVariable Long feedId,
            HttpServletRequest httpServletRequest
    ){
        return feedService.getFeed(feedId,httpServletRequest);

    }
    // 피드 삭제
    @DeleteMapping("/feeds/{feedId}")
    @Timed(value = "Delete feed", description = "Time to delete feed")
    public ResponseEntity<?> deleteFeed(
            @PathVariable Long feedId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        User user = userDetails.getUser();
        feedService.deleteFeed(feedId,user);
        return ResponseEntity.ok().body("피드 삭제 완료");
    }
    // 피드 검색
    @GetMapping("/search")
    @Timed(value = "Search page", description = "Time to Get search response")
    public ResponseEntity<?> searchFeed(@RequestParam String keyword){
        return feedService.searchFeed(keyword);
    }
    @GetMapping("/search/more")
    public ResponseEntity<?> searchMoreFeed(@RequestParam String category,@RequestParam String keyword){
        return feedService.searchMoreFeed(keyword,category);
    }

}
