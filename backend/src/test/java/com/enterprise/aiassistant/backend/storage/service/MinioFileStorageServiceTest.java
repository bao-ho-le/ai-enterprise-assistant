package com.enterprise.aiassistant.backend.storage.service;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.FileStorageException;
import com.enterprise.aiassistant.backend.storage.dto.response.StoredFileDto;
import com.enterprise.aiassistant.backend.storage.mapper.StoredFileMapper;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MinioFileStorageService} with a mocked {@link MinioClient}: object-key
 * generation, bucket usage and exception translation. Real MinIO behaviour is covered by
 * {@code MinioFileStorageServiceIT}.
 */
class MinioFileStorageServiceTest {

    private static final String BUCKET = "test-files";

    private final MinioClient minioClient = mock(MinioClient.class);

    private final StoredFileMapper storedFileMapper = new StoredFileMapper();

    private MinioFileStorageService service;

    @BeforeEach
    void setUp() {

        service = new MinioFileStorageService(minioClient, storedFileMapper);
        ReflectionTestUtils.setField(service, "bucket", BUCKET);
    }

    @Test
    void shouldRejectNullFile() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.store(null)),
                ErrorCode.EMPTY_FILE
        );

        verifyNoInteractions(minioClient);
    }

    @Test
    void shouldRejectEmptyFile() {

        MultipartFile empty = new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0]);

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.store(empty)),
                ErrorCode.EMPTY_FILE
        );

        verifyNoInteractions(minioClient);
    }

    @Test
    void shouldUploadFileUnderGeneratedObjectKey() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", new byte[]{1, 2, 3});

        StoredFileDto dto = service.store(file);

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());

        PutObjectArgs args = captor.getValue();

        assertThat(args.bucket()).isEqualTo(BUCKET);
        assertThat(args.object()).endsWith("-report.pdf");
        assertThat(args.object()).isNotEqualTo("report.pdf");

        assertThat(dto.getOriginalName()).isEqualTo("report.pdf");
        assertThat(dto.getBucket()).isEqualTo(BUCKET);
        assertThat(dto.getObjectKey()).isEqualTo(args.object());
        assertThat(dto.getContentType()).isEqualTo("application/pdf");
        assertThat(dto.getSize()).isEqualTo(3L);
    }

    @Test
    void shouldGenerateDistinctObjectKeysForIdenticalContent() throws Exception {

        byte[] content = {9, 9, 9};
        MockMultipartFile file = new MockMultipartFile("file", "same.pdf", "application/pdf", content);

        String firstKey = service.store(file).getObjectKey();
        String secondKey = service.store(file).getObjectKey();

        assertThat(firstKey).isNotEqualTo(secondKey);
    }

    @Test
    void shouldTranslateUploadFailure() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", new byte[]{1});

        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new IllegalStateException("minio down"));

        Throwable thrown = catchThrowable(() -> service.store(file));

        ErrorCodeAssertions.assertBusinessErrorOfType(
                thrown, FileStorageException.class, ErrorCode.FILE_UPLOAD_FAILED);
        assertThat(thrown.getCause()).hasMessage("minio down");
    }

    @Test
    void shouldLoadObjectAsResource() throws Exception {

        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenReturn(mock(GetObjectResponse.class));

        Resource resource = service.loadAsResource(BUCKET, "key-1");

        ArgumentCaptor<GetObjectArgs> captor = ArgumentCaptor.forClass(GetObjectArgs.class);
        verify(minioClient).getObject(captor.capture());

        assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
        assertThat(captor.getValue().object()).isEqualTo("key-1");
        assertThat(resource).isInstanceOf(InputStreamResource.class);
    }

    @Test
    void shouldTranslateReadFailure() throws Exception {

        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new IllegalStateException("missing"));

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.loadAsResource(BUCKET, "missing-key")),
                ErrorCode.FILE_STORAGE_READ_FAILED
        );
    }
}
