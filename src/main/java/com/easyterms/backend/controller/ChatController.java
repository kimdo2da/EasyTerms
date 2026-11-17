
package com.easyterms.backend.controller;

import com.easyterms.backend.service.ChatService;
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

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // AI에게 질문하기
    @PostMapping
    public Map<String, Object> ask(
            @PathVariable Long documentId,
            @RequestParam Long userId,
            @Valid @RequestBody ChatRequest request
    ) {
        return chatService.ask(
                documentId, userId, request.question());
    }

    // 이전 대화 기록 조회
    @GetMapping
    public List<Map<String, Object>> getHistory(
            @PathVariable Long documentId,
            @RequestParam Long userId
    ) {
        return chatService.getHistory(documentId, userId);
    }

    public record ChatRequest(
            @NotBlank
            @Size(max = 500)
            String question
    ) {}
}
