package com.enterprise.aiassistant.backend.folder.dto.response;

import com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderContentsResponse {

    // Folder đang xem, luôn có giá trị (kể cả khi gọi /root/contents)
    private FolderResponse currentFolder;

    // Đường dẫn từ root -> currentFolder, dùng để render breadcrumb trên UI
    private List<BreadcrumbItem> breadcrumb;

    // Các thư mục con nằm trực tiếp bên trong
    private List<FolderResponse> subfolders;

    // Các tài liệu (file) nằm trực tiếp bên trong, có phân trang
    private Page<DocumentListResponse> documents;
}
