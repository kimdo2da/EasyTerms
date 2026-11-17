
package com.easyterms.backend.service;

import com.easyterms.backend.entity.Document;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

@Service
public class AnalysisService {

    private final DocumentService documentService;
    private final GeminiService geminiService;
    private final JdbcTemplate jdbc;
    private final JsonMapper mapper;
    private final TransactionTemplate transaction;

    public AnalysisService(
            DocumentService documentService,
            GeminiService geminiService,
            JdbcTemplate jdbc,
            JsonMapper mapper,
            PlatformTransactionManager transactionManager
    ) {
        this.documentService = documentService;
        this.geminiService = geminiService;
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    // Gemini 분석 후 DB 저장
    public Map<String, Object> analyze(Long documentId, Long userId) {

        Document document =
                documentService.findOne(documentId, userId);

        String json = geminiService.analyze(
                document.getDocumentType(),
                document.getContent()
        );

        JsonNode result;

        try {
            result = mapper.readTree(json);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI 분석 결과가 올바른 JSON이 아닙니다."
            );
        }

        if (!result.isObject()
                || !result.path("easyExplanation").isTextual()
                || result.path("easyExplanation").asText().isBlank()
                || !result.path("keyConditions").isArray()
                || !result.path("checkItems").isArray()
                || !result.path("terms").isArray()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "AI 분석 결과 형식이 잘못되었습니다."
            );
        }

        transaction.executeWithoutResult(status -> {

            String explanation =
                    result.path("easyExplanation").asText();

            // 기존 분석이 있으면 갱신, 없으면 새로 생성
            jdbc.update("""
                    INSERT INTO analysis_results
                        (document_id, easy_explanation, status)
                    VALUES (?, ?, 'COMPLETED')
                    ON DUPLICATE KEY UPDATE
                        easy_explanation = ?,
                        status = 'COMPLETED',
                        error_message = NULL
                    """, documentId, explanation, explanation);

            Long analysisId = jdbc.queryForObject(
                    "SELECT id FROM analysis_results WHERE document_id = ?",
                    Long.class,
                    documentId
            );

            // 재분석 시 기존 세부 항목을 교체
            jdbc.update(
                    "DELETE FROM key_conditions WHERE analysis_id = ?",
                    analysisId
            );
            jdbc.update(
                    "DELETE FROM check_items WHERE analysis_id = ?",
                    analysisId
            );
            jdbc.update(
                    "DELETE FROM terms WHERE analysis_id = ?",
                    analysisId
            );

            for (JsonNode item : result.path("keyConditions")) {
                jdbc.update("""
                        INSERT INTO key_conditions
                            (analysis_id, item_name, item_value, source_excerpt)
                        VALUES (?, ?, ?, ?)
                        """,
                        analysisId,
                        item.path("itemName").asText(""),
                        item.path("itemValue").asText(""),
                        item.path("sourceExcerpt").asText("")
                );
            }

            for (JsonNode item : result.path("checkItems")) {
                jdbc.update("""
                        INSERT INTO check_items
                            (analysis_id, title, explanation, source_excerpt)
                        VALUES (?, ?, ?, ?)
                        """,
                        analysisId,
                        item.path("title").asText(""),
                        item.path("explanation").asText(""),
                        item.path("sourceExcerpt").asText("")
                );
            }

            for (JsonNode item : result.path("terms")) {
                jdbc.update("""
                        INSERT INTO terms
                            (analysis_id, term, explanation, source_excerpt)
                        VALUES (?, ?, ?, ?)
                        """,
                        analysisId,
                        item.path("term").asText(""),
                        item.path("explanation").asText(""),
                        item.path("sourceExcerpt").asText("")
                );
            }
        });

        return getSaved(documentId, userId);
    }

    // 저장된 분석 결과 조회
    public Map<String, Object> getSaved(Long documentId, Long userId) {

        documentService.findOne(documentId, userId);

        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, easy_explanation
                FROM analysis_results
                WHERE document_id = ?
                  AND status = 'COMPLETED'
                """, documentId);

        if (rows.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "저장된 분석 결과가 없습니다."
            );
        }

        Long analysisId =
                ((Number) rows.get(0).get("id")).longValue();

        String explanation =
                (String) rows.get(0).get("easy_explanation");

        var conditions = jdbc.query("""
                SELECT item_name, item_value,
                       COALESCE(source_excerpt, '') AS source_excerpt
                FROM key_conditions
                WHERE analysis_id = ?
                ORDER BY id
                """,
                (rs, n) -> Map.of(
                        "itemName", rs.getString("item_name"),
                        "itemValue", rs.getString("item_value"),
                        "sourceExcerpt", rs.getString("source_excerpt")
                ),
                analysisId
        );

        var checks = jdbc.query("""
                SELECT title, explanation,
                       COALESCE(source_excerpt, '') AS source_excerpt
                FROM check_items
                WHERE analysis_id = ?
                ORDER BY id
                """,
                (rs, n) -> Map.of(
                        "title", rs.getString("title"),
                        "explanation", rs.getString("explanation"),
                        "sourceExcerpt", rs.getString("source_excerpt")
                ),
                analysisId
        );

        var terms = jdbc.query("""
                SELECT term, explanation,
                       COALESCE(source_excerpt, '') AS source_excerpt
                FROM terms
                WHERE analysis_id = ?
                ORDER BY id
                """,
                (rs, n) -> Map.of(
                        "term", rs.getString("term"),
                        "explanation", rs.getString("explanation"),
                        "sourceExcerpt", rs.getString("source_excerpt")
                ),
                analysisId
        );

        return Map.of(
                "documentId", documentId,
                "easyExplanation", explanation,
                "keyConditions", conditions,
                "checkItems", checks,
                "terms", terms
        );
    }
}
