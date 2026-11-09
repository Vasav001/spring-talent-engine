package com.vasav.springtalentengine.ingestion;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.time.Duration;

@Component
class ResumeStorage {

    private final S3Template s3Template;
    private final String bucket;

    ResumeStorage(S3Template s3Template, @Value("${app.storage.bucket}") String bucket) {
        this.s3Template = s3Template;
        this.bucket = bucket;
    }

    void store(String key, byte[] content, String contentType) {
        s3Template.upload(bucket, key, new ByteArrayInputStream(content), ObjectMetadata.builder().contentType(contentType).build());
    }

    URL uploadUrl(String key, Duration validFor) {
        return s3Template.createSignedPutURL(bucket, key, validFor, null, null);
    }

    boolean exists(String key) {
        return s3Template.objectExists(bucket, key);
    }

    byte[] read(String key) {
        S3Resource resource = s3Template.download(bucket, key);
        try {
            if (resource.contentLength() > ResumeFiles.MAX_SIZE_BYTES) {
                throw new ResumeRejectedException("File is larger than 5 MB");
            }
            try (InputStream input = resource.getInputStream()) {
                return input.readAllBytes();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
