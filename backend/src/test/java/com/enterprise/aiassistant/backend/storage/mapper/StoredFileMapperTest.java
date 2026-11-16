package com.enterprise.aiassistant.backend.storage.mapper;

import com.enterprise.aiassistant.backend.storage.dto.response.StoredFileDto;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StoredFileMapper}.
 */
class StoredFileMapperTest {

    private final StoredFileMapper mapper = new StoredFileMapper();

    @Test
    void shouldMapMultipartFileToDto() {

        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.txt", "text/plain", new byte[]{1, 2, 3, 4});

        StoredFileDto dto = mapper.toDto(file, "uuid-notes.txt", "test-files");

        assertThat(dto.getOriginalName()).isEqualTo("notes.txt");
        assertThat(dto.getStoredName()).isEqualTo("uuid-notes.txt");
        assertThat(dto.getObjectKey()).isEqualTo("uuid-notes.txt");
        assertThat(dto.getBucket()).isEqualTo("test-files");
        assertThat(dto.getContentType()).isEqualTo("text/plain");
        assertThat(dto.getSize()).isEqualTo(4L);
    }
}
