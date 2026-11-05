package com.vasav.springtalentengine.ingestion;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.stream.Collectors;

@Component
class ResumeTextExtractor {

    private static final int MAX_TEXT_LENGTH = 30_000;

    String extract(byte[] content) {
        String text = new TikaDocumentReader(new ByteArrayResource(content)).read().stream()
                .map(Document::getText)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("\n"))
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n\\s*\\n+", "\n\n")
                .strip();
        return text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH) : text;
    }
}
