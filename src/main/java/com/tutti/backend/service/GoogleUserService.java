package com.tutti.backend.service;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import com.tutti.backend.dto.google.GoogleOAuthRequest;
import com.tutti.backend.dto.google.GoogleOAuthResponse;
import com.tutti.backend.dto.google.GoogleUserInfoDto;
import com.tutti.backend.dto.google.GoogleUserResponseDto;
import com.tutti.backend.repository.UserRepository;
import com.tutti.backend.security.UserDetailsImpl;
import com.tutti.backend.security.jwt.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.tutti.backend.domain.User;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleUserService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;


    public GoogleUserResponseDto googleLogin(String code) throws JsonProcessingException {
        //HTTP Request를 위한 RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        // 1. "인가 코드"로 "액세스 토큰" 요청
        String accessToken = getAccessToken(restTemplate, code);

        // 2. "액세스 토큰"으로 "구글 사용자 정보" 가져오기
        GoogleUserInfoDto snsUserInfoDto = getGoogleUserInfo(restTemplate, accessToken);

        // 3. "구글 사용자 정보"로 필요시 회원가입  및 이미 같은 이메일이 있으면 기존회원으로 로그인
        Optional<User> googleUser = registerGoogleOrUpdateGoogle(snsUserInfoDto);

        // 4. 강제 로그인 처리
        final String AUTH_HEADER = "Authorization";
        final String TOKEN_TYPE = "BEARER";

        String jwt_token = forceLogin(googleUser.get()); // 로그인처리 후 토큰 받아오기
        HttpHeaders headers = new HttpHeaders();
        headers.set(AUTH_HEADER, TOKEN_TYPE + " " + jwt_token);
        GoogleUserResponseDto googleUserResponseDto = GoogleUserResponseDto.builder()
                .token(TOKEN_TYPE + " " + jwt_token)
                .email(googleUser.get().getEmail())
                .build();
        System.out.println("Google user's token : " + TOKEN_TYPE + " " + jwt_token);
        System.out.println("LOGIN SUCCESS!");
        return googleUserResponseDto;
    }


    private String getAccessToken(RestTemplate restTemplate, String code) throws JsonProcessingException {

        //Google OAuth Access Token 요청을 위한 파라미터 세팅
        GoogleOAuthRequest googleOAuthRequestParam = GoogleOAuthRequest
                .builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .code(code)
                //.redirectUri("https://memegle.xyz/redirect/google")
                //.redirectUri("http://localhost:3000/redirect/google")
                .redirectUri("http://localhost:8080/api/user/google/callback")
                .grantType("authorization_code").build();


        //JSON 파싱을 위한 기본값 세팅
        //요청시 파라미터는 스네이크 케이스로 세팅되므로 Object mapper에 미리 설정해준다.
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        //AccessToken 발급 요청
        ResponseEntity<String> resultEntity = restTemplate.postForEntity("https://oauth2.googleapis.com/token", googleOAuthRequestParam, String.class);

        //Token Request
        GoogleOAuthResponse result = mapper.readValue(resultEntity.getBody(), new TypeReference<GoogleOAuthResponse>() {
        });

        String jwtToken = result.getId_token();

        return jwtToken;
    }


    private GoogleUserInfoDto getGoogleUserInfo(RestTemplate restTemplate, String jwtToken) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String requestUrl = UriComponentsBuilder.fromHttpUrl("https://oauth2.googleapis.com/tokeninfo")
                .queryParam("id_token", jwtToken).encode().toUriString();

        String resultJson = restTemplate.getForObject(requestUrl, String.class);

        Map<String,String> userInfo = mapper.readValue(resultJson, new TypeReference<Map<String, String>>(){});

        
        GoogleUserInfoDto googleUserInfoDto = GoogleUserInfoDto.builder()
                .email(userInfo.get("email"))
                .nickname(userInfo.get("name"))
                .profileImage(userInfo.get("picture"))
                .build();

        return googleUserInfoDto;
    }


    private Optional<User> registerGoogleOrUpdateGoogle(GoogleUserInfoDto googleUserInfoDto) {
        Optional<User> sameUser = userRepository.findByEmail(googleUserInfoDto.getEmail());

        if (!sameUser.isPresent()) {
            return registerGoogleUserIfNeeded(googleUserInfoDto);
        }
        else {
           return updateGoogleUser(sameUser, googleUserInfoDto);
        }

    }

    private Optional<User> registerGoogleUserIfNeeded(GoogleUserInfoDto googleUserInfoDto) {

        // DB 에 중복된 google Id 가 있는지 확인
        String googleUserId = googleUserInfoDto.getEmail();

        Optional<User> googleUser = userRepository.findByEmail(googleUserId);

        if (googleUser == null) {
            // 회원가입
            // username: google ID(email)
            String email = googleUserInfoDto.getEmail();

            String artist = googleUserInfoDto.getNickname();
            // profileImage: google profile image
            String profileImage = googleUserInfoDto.getProfileImage();

            // password: random UUID
            String password = UUID.randomUUID().toString();
            String encodedPassword = passwordEncoder.encode(password);

            googleUser = Optional.ofNullable(User.builder()
                    .email(email)
                    .password(encodedPassword)
                    .artist(artist)
                    .build());
            userRepository.save(googleUser.get());
        }

        return googleUser;
    }

    private Optional<User> updateGoogleUser(Optional<User> sameUser, GoogleUserInfoDto googleUserInfoDto) {
        if (sameUser.get().getEmail() == null) {
            System.out.println("중복");
            sameUser.get().setEmail(googleUserInfoDto.getEmail());
            userRepository.save(sameUser.get());
        }
        return sameUser;
    }

    private String forceLogin(User googleUser) {
        UserDetailsImpl userDetails = new UserDetailsImpl(googleUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return JwtTokenUtils.generateJwtToken(userDetails);
    }
}
