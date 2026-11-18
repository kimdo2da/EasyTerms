
package com.easyterms.backend.controller;

import com.easyterms.backend.entity.Document;
import com.easyterms.backend.service.DocumentService;
import com.easyterms.backend.service.FileTextService;
import com.easyterms.backend.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class FileUploadController {

    private final DocumentService documentService;
    private final FileTextService fileTextService;
    private final JwtService jwtService;

    public FileUploadController(
            DocumentService documentService,
            FileTextService fileTextService,
            JwtService jwtService
    ) {
        this.documentService = documentService;
        this.fileTextService = fileTextService;
        this.jwtService = jwtService;
    }

    // PDF 업로드 및 텍스트 추출
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> upload(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @RequestParam Long userId,
            @RequestParam String documentType,
            @RequestParam(required = false) String title,
            @RequestParam("file") MultipartFile file
    ) {
        // 파일 처리 전에 JWT 사용자 검증
        Long verifiedUserId =
                jwtService.getAuthorizedUserId(authorization, userId);

        String filename = fileTextService.getFileName(file);
        String content = fileTextService.extractText(file);

        String documentTitle =
                (title == null || title.isBlank()) ? filename : title;

        Document saved = documentService.createFile(
                verifiedUserId,
                documentTitle,
                documentType,
                content,
                filename
        );

        return Map.of(
                "id", saved.getId(),
                "title", saved.getTitle(),
                "documentType", saved.getDocumentType(),
                "inputType", saved.getInputType(),
                "filename", filename,
                "extractedText", saved.getContent()
        );
    }
}
