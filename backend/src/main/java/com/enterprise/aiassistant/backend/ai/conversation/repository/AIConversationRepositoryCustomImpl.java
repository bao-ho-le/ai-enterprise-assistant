package com.enterprise.aiassistant.backend.ai.conversation.repository;

import com.enterprise.aiassistant.backend.ai.conversation.dto.request.ConversationFilterRequest;
import com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse;
import com.enterprise.aiassistant.backend.ai.conversation.enums.ConversationStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class AIConversationRepositoryCustomImpl implements AIConversationRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<ConversationResponse> filterConversations(
            ConversationFilterRequest filter,
            Pageable pageable
    ) {

        StringBuilder jpql = new StringBuilder("""

                SELECT new com.enterprise.aiassistant.backend.ai.conversation.dto.response.ConversationResponse(
                    c.id,
                    c.title,
                    c.conversationType,
                    c.status,
                    (SELECT COUNT(m) FROM AIMessage m WHERE m.conversation = c),
                    (SELECT MAX(m2.createdAt) FROM AIMessage m2 WHERE m2.conversation = c),
                    c.createdAt,
                    c.updatedAt
                )

                FROM AIConversation c

                WHERE 1=1

                """);

        Map<String, Object> params = new HashMap<>();

        appendFilters(jpql, params, filter);

        if ("oldest".equalsIgnoreCase(filter.getSort())) {
            jpql.append(" ORDER BY c.createdAt ASC");
        } else {
            jpql.append(" ORDER BY c.createdAt DESC");
        }

        TypedQuery<ConversationResponse> query =
                entityManager.createQuery(jpql.toString(), ConversationResponse.class);

        params.forEach(query::setParameter);

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        return new PageImpl<>(
                query.getResultList(),
                pageable,
                countConversations(filter)
        );
    }

    private long countConversations(ConversationFilterRequest filter) {

        StringBuilder jpql = new StringBuilder("""

                SELECT COUNT(c)

                FROM AIConversation c

                WHERE 1=1

                """);

        Map<String, Object> params = new HashMap<>();
        appendFilters(jpql, params, filter);

        TypedQuery<Long> query = entityManager.createQuery(jpql.toString(), Long.class);
        params.forEach(query::setParameter);

        return query.getSingleResult();
    }

    private void appendFilters(
            StringBuilder jpql,
            Map<String, Object> params,
            ConversationFilterRequest filter
    ) {

        if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
            jpql.append(" AND LOWER(c.title) LIKE LOWER(:keyword)");
            params.put("keyword", "%" + filter.getKeyword() + "%");
        }

        if (filter.getConversationType() != null) {
            jpql.append(" AND c.conversationType = :conversationType");
            params.put("conversationType", filter.getConversationType());
        }

        // No status filter given -> hide DELETED conversations by default instead of exposing everything.
        if (filter.getStatus() != null) {
            jpql.append(" AND c.status = :status");
            params.put("status", filter.getStatus());
        } else {
            jpql.append(" AND c.status = :status");
            params.put("status", ConversationStatus.ACTIVE);
        }

        if (filter.getFromDate() != null) {
            jpql.append(" AND c.createdAt >= :fromDate");
            params.put("fromDate", filter.getFromDate());
        }

        if (filter.getToDate() != null) {
            jpql.append(" AND c.createdAt <= :toDate");
            params.put("toDate", filter.getToDate());
        }
    }
}
