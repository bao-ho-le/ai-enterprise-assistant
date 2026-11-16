package com.enterprise.aiassistant.backend.storage.controller;

import com.enterprise.aiassistant.backend.storage.dto.response.StoredFileDto;
import com.enterprise.aiassistant.backend.storage.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the debug {@link FileTestController} upload endpoint.
 */
class FileTestControllerTest {

    private final FileStorageService fileStorageService = mock(FileStorageService.class);

    private final FileTestController controller = new FileTestController(fileStorageService);

    @Test
    void shouldReturnStoredFileMetadata() {

        MultipartFile file = new MockMultipartFile(
                "file", "a.txt", "text/plain", new byte[]{1});

        StoredFileDto expected = StoredFileDto.builder().objectKey("uuid-a.txt").build();

        when(fileStorageService.store(file)).thenReturn(expected);

        ResponseEntity<?> response = controller.upload(file);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(expected);
        verify(fileStorageService).store(file);
    }
}
