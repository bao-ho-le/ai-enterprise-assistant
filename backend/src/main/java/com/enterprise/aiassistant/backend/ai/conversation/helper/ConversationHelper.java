package com.enterprise.aiassistant.backend.ai.conversation.helper;

import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConversationHelper {

    public List<DocumentVersion> filterNewVersions(
            List<DocumentVersion> versions,
            List<Long> alreadyAttachedVersionIds
    ) {
        return versions.stream()
                .filter(version -> !alreadyAttachedVersionIds.contains(version.getId()))
                .toList();
    }
}
