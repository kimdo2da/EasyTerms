
package com.easyterms.backend.controller;

import com.easyterms.backend.service.AnalysisService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    // AI 분석 실행 + DB 저장
    @PostMapping("/{documentId}")
    public Map<String, Object> analyze(
            @PathVariable Long documentId,
            @RequestParam Long userId
    ) {
        return analysisService.analyze(documentId, userId);
    }

    // 기존 AI 분석 결과 조회
    @GetMapping("/{documentId}")
    public Map<String, Object> getSaved(
            @PathVariable Long documentId,
            @RequestParam Long userId
    ) {
        return analysisService.getSaved(documentId, userId);
    }
}
