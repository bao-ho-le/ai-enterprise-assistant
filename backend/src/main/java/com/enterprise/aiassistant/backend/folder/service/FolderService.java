package com.enterprise.aiassistant.backend.folder.service;

import com.enterprise.aiassistant.backend.folder.dto.request.CreateFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.request.MoveFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.request.RenameFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.response.FolderContentsResponse;
import com.enterprise.aiassistant.backend.folder.dto.response.FolderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FolderService {

    FolderResponse createFolder(CreateFolderRequest request);

    FolderResponse renameFolder(Long folderId, RenameFolderRequest request);

    FolderResponse moveFolder(Long folderId, MoveFolderRequest request);

    void deleteFolder(Long folderId);

    FolderResponse getFolderDetail(Long folderId);

    // folderId = null -> lấy nội dung thư mục gốc (root / "My Drive")
    FolderContentsResponse getFolderContents(Long folderId, Pageable pageable);

    // Khôi phục 1 folder đã xoá mềm (status DELETED -> ACTIVE), kèm toàn bộ
    // cây con (folder con + document bên trong) đã bị xoá cùng lượt đó.
    FolderResponse restoreFolder(Long folderId);

    // Xoá vĩnh viễn 1 folder khỏi DB. Chỉ cho phép khi folder đang ở trạng thái
    // DELETED (đã qua bước soft-delete/thùng rác trước đó), đệ quy xuống toàn bộ
    // folder con + document bên trong.
    void hardDeleteFolder(Long folderId);

    // Thùng rác: liệt kê các folder đã bị xoá mềm, mới xoá gần nhất lên trước.
    Page<FolderResponse> getDeletedFolders(Pageable pageable);

    // Tìm kiếm folder theo tên (chỉ trong các folder đang hoạt động).
    Page<FolderResponse> searchFolders(String keyword, Pageable pageable);
}
