
package com.easyterms.backend.service;

import com.easyterms.backend.entity.User;
import com.easyterms.backend.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    private final BCryptPasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public AuthService(
            UserRepository userRepository,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    // 회원가입
    @Transactional
    public AuthResponse register(
            String email, String password, String nickname
    ) {
        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 가입된 이메일입니다."
            );
        }

        String hashedPassword =
                passwordEncoder.encode(password);

        User user = new User(
                normalizedEmail,
                hashedPassword,
                nickname.trim()
        );

        try {
            userRepository.saveAndFlush(user);

        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이메일이 이미 사용 중이거나 입력값이 유효하지 않습니다."
            );
        }

        String token = jwtService.createToken(user.getId());

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                "Bearer",
                token
        );
    }

    // 로그인
    public AuthResponse login(String email, String password) {

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "이메일 또는 비밀번호가 올바르지 않습니다."
                ));

        if (!passwordEncoder.matches(
                password,
                user.getPasswordHash()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "이메일 또는 비밀번호가 올바르지 않습니다."
            );
        }

        String token = jwtService.createToken(user.getId());

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                "Bearer",
                token
        );
    }

    // 로그인 사용자 정보
    public UserResponse getMe(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "존재하지 않는 사용자입니다."
                ));

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname()
        );
    }

    public record AuthResponse(
            Long userId,
            String email,
            String nickname,
            String tokenType,
            String accessToken
    ) {}

    public record UserResponse(
            Long id,
            String email,
            String nickname
    ) {}
}
