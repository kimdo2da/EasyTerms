package com.easyterms.backend.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class FileTextService {

    public String getFileName(MultipartFile file) {
        String original = file.getOriginalFilename();

        if (original == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "파일명이 없습니다.");
        }

        String filename = original.replace("\\", "/");
        filename = filename.substring(filename.lastIndexOf('/') + 1);

        if (filename.isBlank() || filename.length() > 255) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "잘못된 파일명입니다.");
        }

        return filename;
    }

    public String extractText(MultipartFile file) {

        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "파일이 비어 있거나 너무 큽니다.");
        }

        String filename = getFileName(file)
                .toLowerCase(Locale.ROOT);

        try {
            byte[] bytes = file.getBytes();
            String text;

            if (filename.endsWith(".pdf")) {

                // 실제 PDF 형식인지 기본 확인
                if (bytes.length < 5
                        || bytes[0] != '%'
                        || bytes[1] != 'P'
                        || bytes[2] != 'D'
                        || bytes[3] != 'F'
                        || bytes[4] != '-') {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "올바른 PDF 파일이 아닙니다.");
                }

                try (PDDocument document = Loader.loadPDF(bytes)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    text = stripper.getText(document);
                }

            } else if (filename.endsWith(".txt")) {

                text = new String(bytes, StandardCharsets.UTF_8);

            } else {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "PDF 또는 TXT 파일만 지원합니다.");
            }

            if (text.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "추출할 텍스트가 없습니다.");
            }

            return text;

        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "파일을 읽지 못했습니다.", e);
        }
    }
}