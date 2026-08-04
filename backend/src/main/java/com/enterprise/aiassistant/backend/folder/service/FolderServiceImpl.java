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
}
