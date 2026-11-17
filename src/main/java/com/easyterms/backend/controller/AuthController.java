
package com.easyterms.backend.controller;

import com.easyterms.backend.service.AuthService;
import com.easyterms.backend.service.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(
            AuthService authService,
            JwtService jwtService
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    // 회원가입
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthService.AuthResponse register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return authService.register(
                request.email(),
                request.password(),
                request.nickname()
        );
    }

    // 로그인
    @PostMapping("/login")
    public AuthService.AuthResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(
                request.email(),
                request.password()
        );
    }

    // 로그인한 사용자 정보 조회
    @GetMapping("/me")
    public AuthService.UserResponse me(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorization
    ) {
        Long userId = jwtService.getUserId(authorization);

        return authService.getMe(userId);
    }

    public record RegisterRequest(
            @NotBlank
            @Email
            String email,

            @NotBlank
            @Size(min = 8, max = 72)
            String password,

            @NotBlank
            @Size(min = 2, max = 30)
            String nickname
    ) {}

    public record LoginRequest(
            @NotBlank String email,
            @NotBlank String password
    ) {}
}
