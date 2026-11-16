package com.enterprise.aiassistant.backend.storage.service;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.FileStorageException;
import com.enterprise.aiassistant.backend.storage.dto.response.StoredFileDto;
import com.enterprise.aiassistant.backend.storage.mapper.StoredFileMapper;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import com.enterprise.aiassistant.backend.support.MinioContainerSupport;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Integration tests against a real MinIO container (pinned image). The test bucket is created by
 * the test support because the production code never creates it.
 */
class MinioFileStorageServiceIT {

    private static MinioClient client;

    private MinioFileStorageService service;

    @BeforeAll
    static void startStorage() throws Exception {

        client = MinioContainerSupport.newClient();
        MinioContainerSupport.ensureBucket(client);
    }

    @BeforeEach
    void setUp() {

        service = new MinioFileStorageService(client, new StoredFileMapper());
        ReflectionTestUtils.setField(service, "bucket", MinioContainerSupport.BUCKET);
    }

    @Test
    void shouldUploadAndDownloadIdenticalBytes() throws Exception {

        byte[] content = "hello minio integration".getBytes(StandardCharsets.UTF_8);
        MultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", content);

        StoredFileDto stored = service.store(file);

        assertThat(stored.getBucket()).isEqualTo(MinioContainerSupport.BUCKET);
        assertThat(stored.getObjectKey()).endsWith("-report.pdf");
        assertThat(stored.getSize()).isEqualTo(content.length);

        Resource resource = service.loadAsResource(stored.getBucket(), stored.getObjectKey());

        try (InputStream inputStream = resource.getInputStream()) {
            assertThat(inputStream.readAllBytes()).isEqualTo(content);
        }
    }

    @Test
    void shouldCreateDistinctObjectKeysForIdenticalContent() {

        MockMultipartFile first = new MockMultipartFile(
                "file", "same.txt", "text/plain", "duplicate".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile second = new MockMultipartFile(
                "file", "same.txt", "text/plain", "duplicate".getBytes(StandardCharsets.UTF_8));

        String firstKey = service.store(first).getObjectKey();
        String secondKey = service.store(second).getObjectKey();

        assertThat(firstKey).isNotEqualTo(secondKey);
        assertThat(firstKey).endsWith("-same.txt");
        assertThat(secondKey).endsWith("-same.txt");
    }

    @Test
    void shouldReportReadFailureForMissingObject() {

        Throwable thrown = catchThrowable(() -> {
            Resource resource = service.loadAsResource(MinioContainerSupport.BUCKET, "does-not-exist");
            try (InputStream inputStream = resource.getInputStream()) {
                inputStream.readAllBytes();
            }
        });

        assertThat(thrown).isNotNull();
    }

    @Test
    void shouldTranslateConnectionFailureOnUpload() {

        MinioClient unreachable = MinioClient.builder()
                .endpoint("http://127.0.0.1:1")
                .credentials(MinioContainerSupport.ACCESS_KEY, MinioContainerSupport.SECRET_KEY)
                .build();

        MinioFileStorageService broken = new MinioFileStorageService(unreachable, new StoredFileMapper());
        ReflectionTestUtils.setField(broken, "bucket", MinioContainerSupport.BUCKET);

        MultipartFile file = new MockMultipartFile(
                "file", "a.txt", "text/plain", new byte[]{1});

        Throwable thrown = catchThrowable(() -> broken.store(file));

        ErrorCodeAssertions.assertBusinessErrorOfType(
                thrown, FileStorageException.class, ErrorCode.FILE_UPLOAD_FAILED);
    }

    @Test
    void shouldRejectEmptyUploadWithoutContactingStorage() {

        MultipartFile empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.store(empty)),
                ErrorCode.EMPTY_FILE
        );
    }
}
