
package com.easyterms.backend.service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${app.jwt.secret-base64}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    // 로그인 성공 시 JWT 발급 (12시간 유효)
    public String createToken(Long userId) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(12, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();
    }

    // Authorization 헤더의 JWT 검증 및 사용자 ID 추출
    public Long getUserId(String authorization) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw unauthorized();
        }

        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            throw unauthorized();
        }

        try {
            String subject = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();

            return Long.parseLong(subject);

        } catch (JwtException | IllegalArgumentException e) {
            throw unauthorized();
        }
    }

    // 요청 userId와 JWT의 실제 사용자 ID 비교
    public Long getAuthorizedUserId(
            String authorization,
            Long requestedUserId
    ) {
        Long authenticatedUserId = getUserId(authorization);

        if (requestedUserId == null
                || !authenticatedUserId.equals(requestedUserId)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "다른 사용자의 데이터에는 접근할 수 없습니다."
            );
        }

        // 이후 서비스에서는 JWT로 검증한 ID만 사용
        return authenticatedUserId;
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "로그인이 필요하거나 토큰이 만료되었습니다."
        );
    }
}
