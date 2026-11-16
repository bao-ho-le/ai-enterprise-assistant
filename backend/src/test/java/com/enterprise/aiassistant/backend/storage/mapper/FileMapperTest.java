package com.enterprise.aiassistant.backend.storage.mapper;

import com.enterprise.aiassistant.backend.storage.dto.response.StoredFileDto;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FileMapper}: metadata propagation and extension parsing.
 */
class FileMapperTest {

    private final FileMapper mapper = new FileMapper();

    @BeforeEach
    void setUp() {

        ReflectionTestUtils.setField(mapper, "bucket", "test-files");
        ReflectionTestUtils.setField(mapper, "provider", "MINIO");
    }

    @Test
    void shouldMapStoredFileToEntity() {

        StoredFileDto dto = StoredFileDto.builder()
                .originalName("Report.PDF")
                .storedName("uuid-Report.PDF")
                .objectKey("uuid-Report.PDF")
                .contentType("application/pdf")
                .size(2048L)
                .build();

        FileEntity entity = mapper.toFileEntity(dto);

        assertThat(entity.getOriginalFilename()).isEqualTo("Report.PDF");
        assertThat(entity.getStoredFilename()).isEqualTo("uuid-Report.PDF");
        assertThat(entity.getObjectKey()).isEqualTo("uuid-Report.PDF");
        assertThat(entity.getBucketName()).isEqualTo("test-files");
        assertThat(entity.getStorageProvider()).isEqualTo("MINIO");
        assertThat(entity.getMimeType()).isEqualTo("application/pdf");
        assertThat(entity.getFileSize()).isEqualTo(2048L);
        assertThat(entity.getExtension()).isEqualTo("pdf");
    }

    @Test
    void shouldReturnNullExtensionWhenFilenameHasNoDot() {

        StoredFileDto dto = StoredFileDto.builder().originalName("README").build();

        assertThat(mapper.toFileEntity(dto).getExtension()).isNull();
    }

    @Test
    void shouldReturnNullExtensionWhenFilenameIsNull() {

        StoredFileDto dto = StoredFileDto.builder().originalName(null).build();

        assertThat(mapper.toFileEntity(dto).getExtension()).isNull();
    }

    @Test
    void shouldReturnNullExtensionForTrailingDot() {

        StoredFileDto dto = StoredFileDto.builder().originalName("archive.").build();

        assertThat(mapper.toFileEntity(dto).getExtension()).isEmpty();
    }
}
