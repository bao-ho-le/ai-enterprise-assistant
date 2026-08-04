package com.enterprise.aiassistant.backend.folder.repository;

import com.enterprise.aiassistant.backend.folder.entity.Folder;
import com.enterprise.aiassistant.backend.folder.enums.FolderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FolderRepository extends JpaRepository<Folder, Long>,
        FolderRepositoryCustom {

    List<Folder> findByParentIsNullAndStatusOrderByNameAsc(FolderStatus status);

    List<Folder> findByParentIdAndStatusOrderByNameAsc(Long parentId, FolderStatus status);

    // parent == null sẽ tự động được Spring Data JPA dịch thành "parent IS NULL"
    boolean existsByNameAndParentAndStatus(String name, Folder parent, FolderStatus status);

    boolean existsByNameAndParentAndStatusAndIdNot(String name, Folder parent, FolderStatus status, Long id);
}
