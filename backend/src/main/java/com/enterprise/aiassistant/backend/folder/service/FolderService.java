package com.enterprise.aiassistant.backend.folder.service;

import com.enterprise.aiassistant.backend.folder.dto.request.CreateFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.request.MoveFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.request.RenameFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.response.FolderContentsResponse;
import com.enterprise.aiassistant.backend.folder.dto.response.FolderResponse;
import org.springframework.data.domain.Pageable;

public interface FolderService {

    FolderResponse createFolder(CreateFolderRequest request);

    FolderResponse renameFolder(Long folderId, RenameFolderRequest request);

    FolderResponse moveFolder(Long folderId, MoveFolderRequest request);

    void deleteFolder(Long folderId);

    FolderResponse getFolderDetail(Long folderId);

    // folderId = null -> lấy nội dung thư mục gốc (root / "My Drive")
    FolderContentsResponse getFolderContents(Long folderId, Pageable pageable);
}
