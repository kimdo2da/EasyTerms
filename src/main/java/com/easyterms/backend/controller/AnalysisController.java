
package com.easyterms.backend.controller;

import com.easyterms.backend.service.AnalysisService;
import com.easyterms.backend.service.JwtService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final JwtService jwtService;

    public AnalysisController(
            AnalysisService analysisService,
            JwtService jwtService
    ) {
        this.analysisService = analysisService;
        this.jwtService = jwtService;
    }

    // 본인 문서 AI 분석
    @PostMapping("/{documentId}")
    public Map<String, Object> analyze(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long documentId,
            @RequestParam Long userId
    ) {
        Long verifiedUserId =
                jwtService.getAuthorizedUserId(authorization, userId);

        return analysisService.analyze(
                documentId,
                verifiedUserId
        );
    }

    // 본인 문서의 저장된 분석 결과 조회
    @GetMapping("/{documentId}")
    public Map<String, Object> getSaved(
            @RequestHeader(value = "Authorization", required = false)
            String authorization,
            @PathVariable Long documentId,
            @RequestParam Long userId
    ) {
        Long verifiedUserId =
                jwtService.getAuthorizedUserId(authorization, userId);

        return analysisService.getSaved(
                documentId,
                verifiedUserId
        );
    }
}
