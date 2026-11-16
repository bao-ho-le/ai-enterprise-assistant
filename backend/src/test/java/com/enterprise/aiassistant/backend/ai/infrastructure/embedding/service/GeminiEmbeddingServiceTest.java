package com.enterprise.aiassistant.backend.ai.infrastructure.embedding.service;

import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.config.GeminiProperties;
import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.dto.EmbeddingResult;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.ai.infrastructure.embedding.config.GeminiEmbeddingModelConfig;
import com.enterprise.aiassistant.backend.support.ErrorCodeAssertions;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GeminiEmbeddingService}. The LangChain4j {@link EmbeddingModel} is mocked,
 * so no API key and no network access are involved.
 */
class GeminiEmbeddingServiceTest {

    private static final float[] VECTOR = {0.1f, 0.2f, 0.3f, 0.4f};

    private final EmbeddingModel embeddingModel = mock(EmbeddingModel.class);

    private GeminiProperties properties;

    private GeminiEmbeddingService service;

    @BeforeEach
    void setUp() {

        properties = new GeminiProperties();
        properties.setApiKey("test-key-not-used");
        properties.setEmbeddingModel("gemini-embedding-001");

        service = new GeminiEmbeddingService(embeddingModel, properties);
    }

    @Test
    void shouldRejectNullTextWithoutCallingTheModel() {

        ErrorCodeAssertions.assertBusinessErrorOfType(
                catchThrowable(() -> service.embed(null)),
                com.enterprise.aiassistant.backend.common.exception.business_exception.EmbeddingException.class,
                ErrorCode.EMBEDDING_TEXT_REQUIRED
        );

        verifyNoInteractions(embeddingModel);
    }

    @Test
    void shouldRejectBlankTextWithoutCallingTheModel() {

        ErrorCodeAssertions.assertBusinessError(
                catchThrowable(() -> service.embed("   ")),
                ErrorCode.EMBEDDING_TEXT_REQUIRED
        );

        verifyNoInteractions(embeddingModel);
    }

    @Test
    void shouldMapSuccessfulEmbeddingResponse() {

        when(embeddingModel.embed("hello world"))
                .thenReturn(Response.from(Embedding.from(VECTOR)));

        EmbeddingResult result = service.embed("hello world");

        assertThat(result.getVector()).containsExactly(VECTOR);
        assertThat(result.getDimension()).isEqualTo(VECTOR.length);
        assertThat(result.getModel()).isEqualTo("gemini-embedding-001");
        // 11 characters / 4 = 2 estimated tokens
        assertThat(result.getInputTokens()).isEqualTo(2);

        verify(embeddingModel).embed("hello world");
    }

    @Test
    void shouldEstimateAtLeastOneTokenForVeryShortText() {

        when(embeddingModel.embed("a")).thenReturn(Response.from(Embedding.from(VECTOR)));

        assertThat(service.embed("a").getInputTokens()).isEqualTo(1);
    }

    @Test
    void shouldPreferReportedTokenUsageWhenPresent() {

        when(embeddingModel.embed("hello world"))
                .thenReturn(Response.from(Embedding.from(VECTOR), new TokenUsage(17, 0)));

        assertThat(service.embed("hello world").getInputTokens()).isEqualTo(17);
    }

    @Test
    void shouldTranslateModelFailures() {

        when(embeddingModel.embed("boom")).thenThrow(new IllegalStateException("provider down"));

        Throwable thrown = catchThrowable(() -> service.embed("boom"));

        ErrorCodeAssertions.assertBusinessError(thrown, ErrorCode.EMBEDDING_FAILED);
        assertThat(thrown.getCause()).hasMessage("provider down");
    }

    @Test
    void shouldExposeConfiguredModelName() {

        assertThat(service.getModelName()).isEqualTo("gemini-embedding-001");
    }

    @Test
    void shouldExposeBedrockEmbeddingResultMapping() {

        EmbeddingResult result = service.toEmbeddingResult(
                "12345678",
                Response.from(Embedding.from(VECTOR)),
                properties
        );

        assertThat(result.getInputTokens()).isEqualTo(2);
        assertThat(result.getDimension()).isEqualTo(4);
    }

    @Test
    void shouldBuildEmbeddingModelBeanFromProperties() {

        GeminiEmbeddingModelConfig config = new GeminiEmbeddingModelConfig(properties);

        assertThat(config.embeddingModel()).isNotNull();
    }
}
