package com.enterprise.aiassistant.backend.folder.repository;

import com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class FolderRepositoryCustomImpl implements FolderRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<DocumentListResponse> getDocumentsInFolder(Long folderId, Pageable pageable) {

        String folderCondition = (folderId == null)
                ? "d.folder IS NULL"
                : "d.folder.id = :folderId";

        String jpql = "SELECT new com.enterprise.aiassistant.backend.document.dto.response.DocumentListResponse(" +
                "d.id, d.title, v.createdAt, f.extension, d.documentType, f.fileSize, v.status, d.status) " +
                "FROM Document d " +
                "JOIN d.currentVersion v " +
                "JOIN v.file f " +
                "WHERE d.status = com.enterprise.aiassistant.backend.document.enums.DocumentStatus.ACTIVE " +
                "AND " + folderCondition + " " +
                "ORDER BY v.createdAt DESC";

        TypedQuery<DocumentListResponse> query =
                entityManager.createQuery(jpql, DocumentListResponse.class);

        if (folderId != null) {
            query.setParameter("folderId", folderId);
        }

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<DocumentListResponse> results = query.getResultList();

        String countJpql = "SELECT COUNT(d) FROM Document d " +
                "WHERE d.status = com.enterprise.aiassistant.backend.document.enums.DocumentStatus.ACTIVE " +
                "AND " + folderCondition;

        TypedQuery<Long> countQuery = entityManager.createQuery(countJpql, Long.class);

        if (folderId != null) {
            countQuery.setParameter("folderId", folderId);
        }

        long total = countQuery.getSingleResult();

        return new PageImpl<>(results, pageable, total);
    }
}
