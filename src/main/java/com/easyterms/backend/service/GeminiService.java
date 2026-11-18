package com.easyterms.backend.service;

import org.springframework.stereotype.Service;

// 기존 AnalysisService를 수정하지 않기 위한 호환 클래스.
// 실제 API 호출은 OpenAiService가 담당한다.
@Service
public class GeminiService {

    private final OpenAiService openAiService;

    public GeminiService(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    public String analyze(String documentType, String text) {
        return openAiService.analyze(documentType, text);
    }
}
