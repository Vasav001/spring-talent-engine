package com.vasav.springtalentengine.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Component
@RequiredArgsConstructor
class ResumeParser {

    private final ResumeTextExtractor textExtractor;

    ParsedResume parse(byte[] content) {
        String rawText = extractText(content);
        return new ParsedResume(sha256(content), PiiMasker.mask(rawText), GithubProfiles.username(rawText).orElse(null));
    }

    private String extractText(byte[] content) {
        String text;
        try {
            text = textExtractor.extract(content);
        } catch (RuntimeException e) {
            log.warn("Could not read resume text", e);
            throw new ResumeRejectedException("Could not read this file");
        }
        if (text.isBlank()) {
            throw new ResumeRejectedException("No readable text found, scanned resumes are not supported");
        }
        return text;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
