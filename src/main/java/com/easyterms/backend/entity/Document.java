
package com.easyterms.backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "document_type", nullable = false, length = 30)
    private String documentType;

    @Column(name = "input_type", nullable = false, length = 20)
    private String inputType;

    @Column(name = "original_filename", length = 255)
    private String originalFilename;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Document(Long userId, String title,
                    String documentType, String content) {
        this.userId = userId;
        this.title = title;
        this.documentType = documentType;
        this.inputType = "TEXT";
        this.content = content;
    }

    // 업로드된 파일 정보 등록
    public void markAsFile(String filename) {
        this.inputType = "FILE";
        this.originalFilename = filename;
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
