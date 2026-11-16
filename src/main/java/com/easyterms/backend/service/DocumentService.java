
package com.easyterms.backend.service;

import com.easyterms.backend.entity.Document;
import com.easyterms.backend.repository.DocumentRepository;
import com.easyterms.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    private static final Set<String> TYPES = Set.of(
            "INSURANCE",
            "LOAN",
            "RENTAL",
            "SUBSCRIPTION",
            "OTHER"
    );

    public DocumentService(DocumentRepository documentRepository,
                           UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    // 업로드된 파일 정보 등록
    public void markAsFile(String filename) {
        this.inputType = "FILE";
        this.originalFilename = filename;
    }

    // 문서 등록
    @Transactional
    public Document create(Long userId, String title,
                           String documentType, String content) {

        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "사용자가 존재하지 않습니다.");
        }

        String type = documentType.trim().toUpperCase();

        if (!TYPES.contains(type)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "지원하지 않는 문서 유형입니다.");
        }

        Document document = new Document(
                userId, title, type, content);

        return documentRepository.save(document);
    }

    // 사용자별 문서 목록 조회
    @Transactional(readOnly = true)
    public List<Document> findAll(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "사용자가 존재하지 않습니다.");
        }

        return documentRepository
                .findByUserIdOrderByCreatedAtDesc(userId);
    }

    // 문서 상세 조회
    @Transactional(readOnly = true)
    public Document findOne(Long id, Long userId) {
        return documentRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."));
    }

    // 문서 삭제
    @Transactional
    public void delete(Long id, Long userId) {
        Document document = findOne(id, userId);
        documentRepository.delete(document);
    }

    // 파일로 등록한 문서 저장
    @Transactional
    public Document createFile(
            Long userId,
            String title,
            String documentType,
            String content,
            String filename
    ) {
        Document document = create(
                userId, title, documentType, content);

        document.markAsFile(filename);

        return document;
    }
}
