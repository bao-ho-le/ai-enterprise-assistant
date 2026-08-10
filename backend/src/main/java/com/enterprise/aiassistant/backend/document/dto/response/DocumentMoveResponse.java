package com.enterprise.aiassistant.backend.document.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentMoveResponse {

    private Long documentId;

    private Long folderId;
}
