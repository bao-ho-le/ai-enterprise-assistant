package com.enterprise.aiassistant.backend.storage.repository;

import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FileRepository extends JpaRepository<FileEntity, Long> {

}
