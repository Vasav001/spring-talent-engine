package com.vasav.springtalentengine.ingestion;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

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
}
