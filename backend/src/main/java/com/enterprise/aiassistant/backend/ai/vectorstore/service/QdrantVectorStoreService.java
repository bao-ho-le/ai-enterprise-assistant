package com.enterprise.aiassistant.backend.ai.vectorstore.service;

import com.enterprise.aiassistant.backend.ai.vectorstore.config.QdrantProperties;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.SearchResult;
import com.enterprise.aiassistant.backend.ai.vectorstore.dto.VectorPoint;
import com.enterprise.aiassistant.backend.ai.vectorstore.helper.QdrantPayloadConverter;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.VectorStoreException;
import com.google.common.util.concurrent.Futures;
import io.qdrant.client.ConditionFactory;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.WithPayloadSelectorFactory;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QdrantVectorStoreService implements VectorStoreService {

    private final QdrantClient qdrantClient;

    private final QdrantProperties properties;

    private final QdrantPayloadConverter payloadConverter;

    @Override
    public void upsert(VectorPoint point) {
        upsert(List.of(point));
    }

    @Override
    public void upsert(List<VectorPoint> points) {

        if (points.isEmpty()) {
            return;
        }

        try {

            List<Points.PointStruct> pointStructs = points.stream()
                    .map(this::toPointStruct)
                    .toList();

            Futures.getUnchecked(
                    qdrantClient.upsertAsync(properties.getCollectionName(), pointStructs)
            );

        } catch (Exception e) {
            throw new VectorStoreException(ErrorCode.VECTOR_UPSERT_FAILED, e);
        }
    }

    @Override
    public void delete(String pointId) {

        try {

            Futures.getUnchecked(
                    qdrantClient.deleteAsync(
                            properties.getCollectionName(),
                            List.of(PointIdFactory.id(Long.parseLong(pointId)))
                    )
            );

        } catch (Exception e) {
            throw new VectorStoreException(ErrorCode.VECTOR_DELETE_FAILED, e);
        }
    }

    @Override
    public List<SearchResult> search(float[] queryVector, int limit, Long documentId) {

        try {

            Points.SearchPoints.Builder request = Points.SearchPoints.newBuilder()
                    .setCollectionName(properties.getCollectionName())
                    .addAllVector(toFloatList(queryVector))
                    .setLimit(limit)
                    .setWithPayload(WithPayloadSelectorFactory.enable(true));

            if (documentId != null) {
                request.setFilter(
                        Points.Filter.newBuilder()
                                .addMust(ConditionFactory.match("documentId", documentId))
                                .build()
                );
            }

            List<Points.ScoredPoint> scoredPoints =
                    Futures.getUnchecked(qdrantClient.searchAsync(request.build()));

            return scoredPoints.stream()
                    .map(this::toSearchResult)
                    .toList();

        } catch (Exception e) {
            throw new VectorStoreException(ErrorCode.VECTOR_SEARCH_FAILED, e);
        }
    }

    private Points.PointStruct toPointStruct(VectorPoint point) {
        return Points.PointStruct.newBuilder()
                .setId(PointIdFactory.id(Long.parseLong(point.getId())))
                .setVectors(VectorsFactory.vectors(point.getVector()))
                .putAllPayload(payloadConverter.toQdrantPayload(point.getPayload()))
                .build();
    }

    private SearchResult toSearchResult(Points.ScoredPoint scoredPoint) {
        return SearchResult.builder()
                .pointId(String.valueOf(scoredPoint.getId().getNum()))
                .score((double) scoredPoint.getScore())
                .payload(payloadConverter.toVectorPayload(scoredPoint.getPayloadMap()))
                .build();
    }

    private static List<Float> toFloatList(float[] vector) {
        List<Float> list = new ArrayList<>(vector.length);
        for (float value : vector) {
            list.add(value);
        }
        return list;
    }
}
