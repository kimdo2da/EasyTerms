
package com.easyterms.backend.controller;

import com.easyterms.backend.service.ChatService;
import com.easyterms.backend.service.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents/{documentId}/chat")
public class ChatController {

    private final ChatService chatService;
    private final JwtService jwtService;

    public ChatController(
            ChatService chatService,
            JwtService jwtService
    ) {
        this.chatService = chatService;
        this.jwtService = jwtService;
    }

    // 본인 문서에 후속 질문
    @PostMapping
    public Map<String, Object> ask(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long documentId,
            @RequestParam Long userId,
            @Valid @RequestBody ChatRequest request
    ) {
        Long verifiedUserId =
                jwtService.getAuthorizedUserId(authorization, userId);

        return chatService.ask(
                documentId,
                verifiedUserId,
                request.question()
        );
    }

    // 본인 문서의 채팅 기록 조회
    @GetMapping
    public List<Map<String, Object>> getHistory(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long documentId,
            @RequestParam Long userId
    ) {
        Long verifiedUserId =
                jwtService.getAuthorizedUserId(authorization, userId);

        return chatService.getHistory(
                documentId,
                verifiedUserId
        );
    }

    public record ChatRequest(
            @NotBlank
            @Size(max = 500)
            String question
    ) {}
}
