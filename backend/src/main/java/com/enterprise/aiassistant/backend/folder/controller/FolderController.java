package com.enterprise.aiassistant.backend.folder.controller;

import com.enterprise.aiassistant.backend.folder.dto.request.CreateFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.request.MoveFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.request.RenameFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.response.FolderContentsResponse;
import com.enterprise.aiassistant.backend.folder.dto.response.FolderResponse;
import com.enterprise.aiassistant.backend.folder.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${api.prefix}/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(
            @RequestBody CreateFolderRequest request
    ) {
        return ResponseEntity.ok(folderService.createFolder(request));
    }

    @PutMapping("/{folderId}")
    public ResponseEntity<FolderResponse> renameFolder(
            @PathVariable Long folderId,
            @RequestBody RenameFolderRequest request
    ) {
        return ResponseEntity.ok(folderService.renameFolder(folderId, request));
    }

    @PutMapping("/{folderId}/move")
    public ResponseEntity<FolderResponse> moveFolder(
            @PathVariable Long folderId,
            @RequestBody MoveFolderRequest request
    ) {
        return ResponseEntity.ok(folderService.moveFolder(folderId, request));
    }

    @DeleteMapping("/{folderId}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long folderId) {
        folderService.deleteFolder(folderId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{folderId}")
    public ResponseEntity<FolderResponse> getFolderDetail(@PathVariable Long folderId) {
        return ResponseEntity.ok(folderService.getFolderDetail(folderId));
    }

    @GetMapping("/{folderId}/contents")
    public ResponseEntity<FolderContentsResponse> getFolderContents(
            @PathVariable Long folderId,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(folderService.getFolderContents(folderId, pageable));
    }

    // Nội dung thư mục gốc ("My Drive")
    @GetMapping("/root/contents")
    public ResponseEntity<FolderContentsResponse> getRootContents(
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(folderService.getFolderContents(null, pageable));
    }
}
