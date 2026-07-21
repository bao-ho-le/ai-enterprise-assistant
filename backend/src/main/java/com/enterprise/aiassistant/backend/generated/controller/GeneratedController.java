
package com.enterprise.aiassistant.backend.generated.controller;

import com.enterprise.aiassistant.backend.generated.dto.request.UpdateGeneratedContentRequest;
import com.enterprise.aiassistant.backend.generated.dto.response.GeneratedContentDetailResponse;
import com.enterprise.aiassistant.backend.generated.dto.response.GeneratedContentResponse;
import com.enterprise.aiassistant.backend.generated.enums.GeneratedDocumentType;
import com.enterprise.aiassistant.backend.generated.service.GeneratedContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/generated-contents")
@RequiredArgsConstructor
public class GeneratedController {

    private final GeneratedContentService generatedContentService;

    /**
     * Lấy danh sách generated content theo dạng Slice.
     * GET /api/v1/generated-contents?page=0&size=20
     * GET /api/v1/generated-contents?generatedType=REPORT&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Slice<GeneratedContentResponse>>
    getGeneratedContents(
            @RequestParam(required = false)
            GeneratedDocumentType generatedType,

            @PageableDefault(size = 20)
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                generatedContentService.getGeneratedContents(
                        generatedType,
                        pageable
                )
        );
    }

    /**
     * Lấy chi tiết generated content theo ID.
     * GET /api/v1/generated-contents/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<GeneratedContentDetailResponse>
    getGeneratedContentById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                generatedContentService.getGeneratedContentById(id)
        );
    }

    /**
     * Cập nhật title và content của generated content.
     *
     * PUT /api/v1/generated-contents/1
     */
    @PutMapping("/{id}")
    public ResponseEntity<GeneratedContentDetailResponse>
    updateGeneratedContent(
            @PathVariable Long id,
            @Valid @RequestBody UpdateGeneratedContentRequest request
    ) {
        return ResponseEntity.ok(
                generatedContentService.updateGeneratedContent(
                        id,
                        request
                )
        );
    }
}

