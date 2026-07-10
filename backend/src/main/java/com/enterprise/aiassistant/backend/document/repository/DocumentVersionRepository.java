package com.enterprise.aiassistant.backend.document.repository;

import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {

}
