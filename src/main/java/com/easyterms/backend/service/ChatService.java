package com.easyterms.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final DocumentService documentService;
    private final JdbcTemplate jdbc;
    private final OpenAiService openAiService;
    private final TransactionTemplate transaction;

    public ChatService(
            DocumentService documentService,
            JdbcTemplate jdbc,
            OpenAiService openAiService,
            PlatformTransactionManager transactionManager
    ) {
        this.documentService = documentService;
        this.jdbc = jdbc;
        this.openAiService = openAiService;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    // 계약서 원문 + 최근 대화로 후속 질문에 답변
    public Map<String, Object> ask(Long documentId, Long userId, String question) {
        var document = documentService.findOne(documentId, userId);

        if (question == null || question.isBlank() || question.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "질문은 1~500자로 입력해주세요."
            );
        }

        if (document.getContent() == null || document.getContent().isBlank()
                || document.getContent().length() > 50000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "질문에 사용할 문서가 없거나 너무 깁니다."
            );
        }

        List<Map<String, Object>> history = jdbc.queryForList("""
                SELECT role, content
                FROM (
                    SELECT id, role, content
                    FROM chat_messages
                    WHERE document_id = ?
                    ORDER BY id DESC
                    LIMIT 10
                ) recent
                ORDER BY id ASC
                """, documentId);

        StringBuilder prompt = new StringBuilder();
        prompt.append("""
                [계약 문서]
                """);
        prompt.append(document.getContent());
        prompt.append("\n[계약 문서 끝]\n\n");
        prompt.append("[이전 대화]\n");

        for (Map<String, Object> message : history) {
            prompt.append(message.get("role"))
                    .append(": ")
                    .append(message.get("content"))
                    .append("\n");
        }

        prompt.append("\n[현재 질문]\n");
        prompt.append(question);

        // 응답 성공 시에만 질문과 답변을 저장한다.
        String answer = openAiService.ask(prompt.toString());

        transaction.executeWithoutResult(status -> {
            jdbc.update("""
                    INSERT INTO chat_messages (document_id, role, content)
                    VALUES (?, 'USER', ?)
                    """, documentId, question);
            jdbc.update("""
                    INSERT INTO chat_messages (document_id, role, content)
                    VALUES (?, 'ASSISTANT', ?)
                    """, documentId, answer);
        });

        return Map.of(
                "documentId", documentId,
                "question", question,
                "answer", answer
        );
    }

    public List<Map<String, Object>> getHistory(Long documentId, Long userId) {
        documentService.findOne(documentId, userId);

        return jdbc.query("""
                SELECT id, role, content, created_at
                FROM chat_messages
                WHERE document_id = ?
                ORDER BY id ASC
                """,
                (rs, rowNum) -> Map.of(
                        "id", rs.getLong("id"),
                        "role", rs.getString("role"),
                        "content", rs.getString("content"),
                        "createdAt", rs.getTimestamp("created_at")
                                .toLocalDateTime().toString()
                ),
                documentId
        );
    }
}
