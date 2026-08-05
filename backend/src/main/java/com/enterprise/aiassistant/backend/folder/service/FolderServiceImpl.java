package com.enterprise.aiassistant.backend.folder.service;

import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.FolderException;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.enums.DocumentStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.folder.dto.request.CreateFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.request.MoveFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.request.RenameFolderRequest;
import com.enterprise.aiassistant.backend.folder.dto.response.FolderContentsResponse;
import com.enterprise.aiassistant.backend.folder.dto.response.FolderResponse;
import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.folder.enums.FolderStatus;
import com.enterprise.aiassistant.backend.folder.helper.FolderHelper;
import com.enterprise.aiassistant.backend.folder.mapper.FolderMapper;
import com.enterprise.aiassistant.backend.folder.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private final FolderRepository folderRepository;

    private final DocumentRepository documentRepository;

    private final FolderMapper folderMapper;

    private final FolderHelper folderHelper;

    @Override
    @Transactional
    public FolderResponse createFolder(CreateFolderRequest request) {

        folderHelper.validateCreateRequest(request);

        Folder parent = null;

        if (request.getParentId() != null) {
            parent = folderRepository.findById(request.getParentId())
                    .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_PARENT_NOT_FOUND));

            folderHelper.validateFolderStatus(parent);
        }

        folderHelper.validateNameNotDuplicated(request.getName(), parent);

        Folder folder = folderMapper.toFolder(request, parent);

        folderRepository.save(folder);

        return folderMapper.toFolderResponse(folder);
    }

    @Override
    @Transactional
    public FolderResponse renameFolder(Long folderId, RenameFolderRequest request) {

        folderHelper.validateFolderId(folderId);
        folderHelper.validateRenameRequest(request);

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_NOT_FOUND));

        folderHelper.validateFolderStatus(folder);

        folderHelper.validateNameNotDuplicated(request.getName(), folder.getParent(), folder.getId());

        folder.setName(request.getName());

        folderRepository.save(folder);

        return folderMapper.toFolderResponse(folder);
    }

    @Override
    @Transactional
    public FolderResponse moveFolder(Long folderId, MoveFolderRequest request) {

        folderHelper.validateFolderId(folderId);

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_NOT_FOUND));

        folderHelper.validateFolderStatus(folder);

        Folder targetParent = null;

        if (request.getTargetParentId() != null) {

            if (request.getTargetParentId().equals(folderId)) {
                throw new FolderException(ErrorCode.FOLDER_CANNOT_MOVE_INTO_ITSELF);
            }

            targetParent = folderRepository.findById(request.getTargetParentId())
                    .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_PARENT_NOT_FOUND));

            folderHelper.validateFolderStatus(targetParent);

            folderHelper.validateNotDescendant(folder, targetParent);
        }

        folderHelper.validateNameNotDuplicated(folder.getName(), targetParent, folder.getId());

        folder.setParent(targetParent);

        folderRepository.save(folder);

        return folderMapper.toFolderResponse(folder);
    }

    @Override
    @Transactional
    public void deleteFolder(Long folderId) {

        folderHelper.validateFolderId(folderId);

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_NOT_FOUND));

        folderHelper.validateFolderStatus(folder);

        // Xóa mềm (soft delete) toàn bộ cây: folder hiện tại + tất cả folder con + tất cả document bên trong,
        // giống hành vi "Move to trash" của Google Drive.
        softDeleteRecursive(folder, LocalDateTime.now());
    }

    private void softDeleteRecursive(Folder folder, LocalDateTime now) {

        folder.setStatus(FolderStatus.DELETED);
        folder.setDeletedAt(now);
        folderRepository.save(folder);

        List<Document> documents =
                documentRepository.findByFolderIdAndStatus(folder.getId(), DocumentStatus.ACTIVE);

        documents.forEach(document -> {
            document.setStatus(DocumentStatus.DELETED);
            document.setDeletedAt(now);
        });

        documentRepository.saveAll(documents);

        List<Folder> children =
                folderRepository.findByParentIdAndStatusOrderByNameAsc(folder.getId(), FolderStatus.ACTIVE);

        for (Folder child : children) {
            softDeleteRecursive(child, now);
        }
    }

    @Override
    public FolderResponse getFolderDetail(Long folderId) {

        folderHelper.validateFolderId(folderId);

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_NOT_FOUND));

        return folderMapper.toFolderResponse(folder);
    }

    @Override
    @Transactional(readOnly = true)
    public FolderContentsResponse getFolderContents(Long folderId, Pageable pageable) {

        Folder currentFolder = null;

        if (folderId != null) {

            folderHelper.validateFolderId(folderId);

            currentFolder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_NOT_FOUND));

            folderHelper.validateFolderStatus(currentFolder);
        }

        List<Folder> subfolders = (folderId == null)
                ? folderRepository.findByParentIsNullAndStatusOrderByNameAsc(FolderStatus.ACTIVE)
                : folderRepository.findByParentIdAndStatusOrderByNameAsc(folderId, FolderStatus.ACTIVE);

        Page<com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse> documents =
                folderRepository.getDocumentsInFolder(folderId, pageable);

        return FolderContentsResponse.builder()
                .currentFolder(folderMapper.toFolderResponse(currentFolder))
                .breadcrumb(currentFolder == null ? List.of() : folderMapper.toBreadcrumb(currentFolder))
                .subfolders(folderMapper.toFolderResponseList(subfolders))
                .documents(documents)
                .build();
    }

    @Override
    @Transactional
    public FolderResponse restoreFolder(Long folderId) {

        folderHelper.validateFolderId(folderId);

        // Không dùng findById + validateFolderStatus (dùng để chặn thao tác trên folder
        // đã xoá) vì restore chính là thao tác dành riêng cho folder đang ở trạng thái DELETED.
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_NOT_FOUND));

        if (folder.getStatus() != FolderStatus.DELETED) {
            throw new FolderException(ErrorCode.FOLDER_NOT_DELETED);
        }

        // Khôi phục tên nếu đang bị trùng với 1 folder khác đã được tạo/đổi tên
        // trong lúc folder này nằm trong thùng rác, để tránh vi phạm unique constraint.
        folderHelper.validateNameNotDuplicated(folder.getName(), folder.getParent(), folder.getId());

        restoreRecursive(folder, LocalDateTime.now());

        return folderMapper.toFolderResponse(folder);
    }

    private void restoreRecursive(Folder folder, LocalDateTime now) {

        folder.setStatus(FolderStatus.ACTIVE);
        folder.setDeletedAt(null);
        folder.setUpdatedAt(now);
        folderRepository.save(folder);

        List<Document> documents =
                documentRepository.findByFolderIdAndStatus(folder.getId(), DocumentStatus.DELETED);

        documents.forEach(document -> {
            document.setStatus(DocumentStatus.ACTIVE);
            document.setDeletedAt(null);
        });

        documentRepository.saveAll(documents);

        // Khôi phục toàn bộ cây con đã bị xoá cùng lượt (mirror của softDeleteRecursive).
        List<Folder> children =
                folderRepository.findByParentIdAndStatusOrderByNameAsc(folder.getId(), FolderStatus.DELETED);

        for (Folder child : children) {
            restoreRecursive(child, now);
        }
    }

    @Override
    @Transactional
    public void hardDeleteFolder(Long folderId) {

        folderHelper.validateFolderId(folderId);

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new FolderException(ErrorCode.FOLDER_NOT_FOUND));

        // Bắt buộc phải qua bước soft-delete (thùng rác) trước, tránh xoá nhầm
        // vĩnh viễn 1 folder đang hoạt động bình thường.
        if (folder.getStatus() != FolderStatus.DELETED) {
            throw new FolderException(ErrorCode.FOLDER_NOT_DELETED);
        }

        hardDeleteRecursive(folder);
    }

    // NOTE: chỉ xoá dữ liệu quan hệ (Folder, Document, DocumentVersion nhờ cascade).
    // Chưa dọn file vật lý trên MinIO cũng như vector trên Qdrant liên quan tới các
    // document bị xoá vĩnh viễn ở đây — cần bổ sung khi có nhu cầu dọn storage/vector store.
    private void hardDeleteRecursive(Folder folder) {

        // Xoá con trước (không lọc theo status để đảm bảo dọn sạch toàn bộ cây,
        // kể cả khi có dữ liệu không đồng bộ status do thao tác thủ công trước đó).
        List<Folder> children = new ArrayList<>(folderRepository.findByParentId(folder.getId()));
        for (Folder child : children) {
            hardDeleteRecursive(child);
        }

        List<Document> documents = documentRepository.findByFolderId(folder.getId());
        documentRepository.deleteAll(documents);

        folderRepository.delete(folder);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FolderResponse> getDeletedFolders(Pageable pageable) {

        return folderRepository.findByStatusOrderByDeletedAtDesc(FolderStatus.DELETED, pageable)
                .map(folderMapper::toFolderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FolderResponse> searchFolders(String keyword, Pageable pageable) {

        folderHelper.validateSearchKeyword(keyword);

        String safeKeyword = keyword.trim();

        return folderRepository
                .findByNameContainingIgnoreCaseAndStatusOrderByNameAsc(safeKeyword, FolderStatus.ACTIVE, pageable)
                .map(folderMapper::toFolderResponse);
    }
}
