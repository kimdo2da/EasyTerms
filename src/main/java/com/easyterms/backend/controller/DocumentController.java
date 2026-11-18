
package com.easyterms.backend.controller;

import com.easyterms.backend.entity.Document;
import com.easyterms.backend.service.DocumentService;
import com.easyterms.backend.service.JwtService;
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
    private final JwtService jwtService;

    public DocumentController(
            DocumentService documentService,
            JwtService jwtService
    ) {
        this.documentService = documentService;
        this.jwtService = jwtService;
    }

    // 문서 등록
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse create(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @Valid @RequestBody DocumentRequest request
    ) {
        Long userId = jwtService.getAuthorizedUserId(
                authorization,
                request.userId()
        );

        Document saved = documentService.create(
                userId,
                request.title(),
                request.documentType(),
                request.content()
        );

        return DocumentResponse.from(saved);
    }

    // 내 문서 목록 조회
    @GetMapping
    public List<DocumentResponse> findAll(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @RequestParam Long userId
    ) {
        Long verifiedUserId =
                jwtService.getAuthorizedUserId(authorization, userId);

        return documentService.findAll(verifiedUserId)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    // 내 문서 상세 조회
    @GetMapping("/{id}")
    public DocumentResponse findOne(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        Long verifiedUserId =
                jwtService.getAuthorizedUserId(authorization, userId);

        return DocumentResponse.from(
                documentService.findOne(id, verifiedUserId)
        );
    }

    // 내 문서 삭제
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        Long verifiedUserId =
                jwtService.getAuthorizedUserId(authorization, userId);

        documentService.delete(id, verifiedUserId);
    }

    public record DocumentRequest(
            @NotNull Long userId,
            @NotBlank String title,
            @NotBlank String documentType,
            @NotBlank String content
    ) {}

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
