
package com.easyterms.backend.controller;

import com.easyterms.backend.entity.Document;
import com.easyterms.backend.service.DocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // 문서 등록
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse create(
            @Valid @RequestBody DocumentRequest request) {

        Document saved = documentService.create(
                request.userId(),
                request.title(),
                request.documentType(),
                request.content()
        );

        return DocumentResponse.from(saved);
    }

    // 문서 목록
    @GetMapping
    public List<DocumentResponse> findAll(
            @RequestParam Long userId) {

        return documentService.findAll(userId)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    // 문서 상세
    @GetMapping("/{id}")
    public DocumentResponse findOne(
            @PathVariable Long id,
            @RequestParam Long userId) {

        return DocumentResponse.from(
                documentService.findOne(id, userId)
        );
    }

    // 문서 삭제
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @RequestParam Long userId) {

        documentService.delete(id, userId);
    }

    // 요청 데이터
    public record DocumentRequest(
            @NotNull Long userId,
            @NotBlank String title,
            @NotBlank String documentType,
            @NotBlank String content
    ) {}

    // 응답 데이터
    public record DocumentResponse(
            Long id,
            Long userId,
            String title,
            String documentType,
            String inputType,
            String content,
            LocalDateTime createdAt
    ) {
        public static DocumentResponse from(Document d) {
            return new DocumentResponse(
                    d.getId(),
                    d.getUserId(),
                    d.getTitle(),
                    d.getDocumentType(),
                    d.getInputType(),
                    d.getContent(),
                    d.getCreatedAt()
            );
        }
    }
}
