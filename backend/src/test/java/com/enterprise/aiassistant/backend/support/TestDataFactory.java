package com.enterprise.aiassistant.backend.support;

import com.enterprise.aiassistant.backend.ai.analytics.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.analytics.usage.repository.AIUsageLogRepository;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversationDocument;
import com.enterprise.aiassistant.backend.ai.chat.conversation.enums.ConversationStatus;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationDocumentRepository;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.chat.memory.entity.ConversationMemory;
import com.enterprise.aiassistant.backend.ai.chat.memory.repository.ConversationMemoryRepository;
import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.chat.message.entity.AIMessageSource;
import com.enterprise.aiassistant.backend.ai.chat.message.enums.AIMessageRole;
import com.enterprise.aiassistant.backend.ai.chat.message.repository.AIMessageRepository;
import com.enterprise.aiassistant.backend.ai.chat.message.repository.AIMessageSourceRepository;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.GeneratedContent;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.entity.Generation;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedDocumentType;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GeneratedType;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.enums.GenerationStatus;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.repository.GeneratedContentRepository;
import com.enterprise.aiassistant.backend.ai.knowledge.generation.repository.GenerationRepository;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.department.repository.DepartmentRepository;
import com.enterprise.aiassistant.backend.document.entity.Document;
import com.enterprise.aiassistant.backend.document.entity.DocumentChunk;
import com.enterprise.aiassistant.backend.document.entity.DocumentText;
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import com.enterprise.aiassistant.backend.document.enums.ExtractionMethod;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentChunkRepository;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.document.repository.DocumentTextRepository;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Role;
import com.enterprise.aiassistant.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

// Hàm tạo dữ liệu mẫu cho integration test. Mỗi hàm lưu thẳng xuống DB qua repository thật
// (mỗi lần save là một transaction riêng, commit luôn), nên request MockMvc sau đó đọc được.
//
// Quy ước để IntegrationTestBase dọn dữ liệu sau mỗi test:
//   - username của user test bắt đầu bằng "it_"
//   - tên department test bắt đầu bằng "IT "
@RequiredArgsConstructor
public class TestDataFactory {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final AIConversationRepository conversationRepository;
    private final GenerationRepository generationRepository;
    private final GeneratedContentRepository generatedContentRepository;
    private final AIUsageLogRepository usageLogRepository;
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentTextRepository documentTextRepository;
    private final AIMessageRepository messageRepository;
    private final AIMessageSourceRepository messageSourceRepository;
    private final ConversationMemoryRepository conversationMemoryRepository;
    private final AIConversationDocumentRepository conversationDocumentRepository;

    // ===================== User / Department =====================

    public User createUser(String name) {
        return createUser(name, null);
    }

    public User createUser(String name, Department department) {
        return userRepository.save(User.builder()
                .username("it_" + name)
                .email(name + "@it.test")
                .fullName("Test " + name)
                .password("not-used") // test đăng nhập bằng loginAs(...), không qua mật khẩu
                .role(Role.EMPLOYEE)
                .department(department)
                .build());
    }

    public Department createDepartment(String name) {
        return departmentRepository.save(Department.builder().name("IT " + name).build());
    }

    // ===================== AI =====================

    public AIConversation createConversation(User owner, ConversationType type) {
        return conversationRepository.save(AIConversation.builder()
                .title("Hội thoại test")
                .user(owner)
                .conversationType(type)
                .build());
    }

    public AIConversation createDeletedConversation(User owner, ConversationType type) {
        return conversationRepository.save(AIConversation.builder()
                .title("Hội thoại đã xóa")
                .user(owner)
                .conversationType(type)
                .status(ConversationStatus.DELETED)
                .deletedAt(LocalDateTime.now())
                .build());
    }

    public AIMessage createMessage(AIConversation conversation, AIMessageRole role, String content) {
        return messageRepository.save(AIMessage.builder()
                .conversation(conversation)
                .role(role)
                .content(content)
                .build());
    }

    // Đoạn tài liệu làm bằng chứng cho một câu trả lời của AI
    public AIMessageSource createMessageSource(AIMessage message, DocumentChunk chunk) {
        return messageSourceRepository.save(AIMessageSource.builder()
                .aiMessage(message)
                .documentChunk(chunk)
                .chunkId(chunk.getId())
                .documentVersionId(chunk.getDocumentVersion().getId())
                .similarityScore(0.9)
                .score(0.9)
                .build());
    }

    public ConversationMemory createMemory(AIConversation conversation) {
        return conversationMemoryRepository.save(ConversationMemory.builder()
                .conversation(conversation)
                .pendingContext("USER: Xin chào")
                .contextCharacterCount(14)
                .updatedAt(LocalDateTime.now())
                .build());
    }

    public AIConversationDocument attachDocument(AIConversation conversation, DocumentVersion version) {
        return conversationDocumentRepository.save(AIConversationDocument.builder()
                .conversation(conversation)
                .documentVersion(version)
                .build());
    }

    public Generation createGeneration(AIConversation conversation, GenerationStatus status) {
        return generationRepository.save(Generation.builder()
                .aiConversation(conversation)
                .generatedType(GeneratedType.EMAIL)
                .title("Lần sinh test")
                .status(status)
                .build());
    }

    // Generated content không lưu chủ sở hữu trực tiếp: nó thuộc về chủ của conversation đã sinh ra nó
    // (qua bảng generations). Vì vậy phải tạo đủ chuỗi conversation → generated content → generation.
    public GeneratedContent createGeneratedContent(User owner, GeneratedDocumentType type, String title, String content) {
        return createGeneratedContent(createConversation(owner, ConversationType.EMAIL_GENERATION), type, title, content);
    }

    public GeneratedContent createGeneratedContent(AIConversation conversation, GeneratedDocumentType type, String title, String content) {
        GeneratedContent generatedContent = generatedContentRepository.save(GeneratedContent.builder()
                .generatedType(type)
                .title(title)
                .content(content)
                .build());

        generationRepository.save(Generation.builder()
                .aiConversation(conversation)
                .generatedContent(generatedContent)
                .generatedType(GeneratedType.valueOf(type.name()))
                .title(title)
                .status(GenerationStatus.COMPLETED)
                .build());

        return generatedContent;
    }

    // createdAt do Hibernate tự gán = thời điểm lưu, nên log luôn thuộc "hôm nay"
    public AIUsageLog createUsageLog(User user, String model, AIUsageStatus status, int inputTokens, int outputTokens) {
        return usageLogRepository.save(AIUsageLog.builder()
                .userId(user.getId())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .conversationType(ConversationType.DOCUMENT_QA)
                .model(model)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(inputTokens + outputTokens)
                .status(status)
                .build());
    }

    public AIUsageLog createUsageLog(User user, AIConversation conversation) {
        return usageLogRepository.save(AIUsageLog.builder()
                .userId(user.getId())
                .aiConversationId(conversation.getId())
                .conversationType(conversation.getConversationType())
                .model("gemini-a")
                .inputTokens(10)
                .outputTokens(5)
                .totalTokens(15)
                .status(AIUsageStatus.SUCCESS)
                .build());
    }

    // ===================== Document =====================

    // Toàn văn đã trích xuất của tài liệu, nguồn dữ liệu của Report/Summary generation
    public DocumentText createDocumentText(DocumentVersion version, String content) {
        return documentTextRepository.save(DocumentText.builder()
                .documentVersion(version)
                .content(content)
                .extractionMethod(ExtractionMethod.DIRECT_TEXT)
                .build());
    }

    public DocumentChunk createChunk(DocumentVersion version, String content) {
        return documentChunkRepository.save(DocumentChunk.builder()
                .documentVersion(version)
                .chunkIndex(0)
                .content(content)
                .pageNumber(3)
                .build());
    }

    // Tài liệu có 1 version READY, user là chủ sở hữu (nên đọc được). Không upload file thật lên MinIO,
    // chỉ tạo bản ghi files vì document_versions.file_id là cột bắt buộc.
    public DocumentVersion createDocument(User owner, String title) {
        // Không tự save file: DocumentVersion.file có cascade ALL nên lưu version sẽ lưu luôn file.
        // Save riêng trước sẽ bị lỗi "Detached entity passed to persist".
        FileEntity file = FileEntity.builder()
                .originalFilename(title + ".pdf")
                .storedFilename(title + ".pdf")
                .storageProvider("MINIO")
                .objectKey("it/" + title + ".pdf")
                .build();

        Document document = documentRepository.save(Document.builder()
                .title(title)
                .documentType(DocumentType.OTHER)
                .owner(owner)
                .department(owner.getDepartment())
                .build());

        DocumentVersion version = documentVersionRepository.save(DocumentVersion.builder()
                .document(document)
                .versionNumber(1)
                .file(file)
                .status(VersionStatus.READY)
                .build());

        // versions có orphanRemoval = true: phải thêm version vào list trước khi save lại,
        // nếu không Hibernate coi version là "mồ côi" và xóa nó đi.
        document.getVersions().add(version);
        document.setCurrentVersion(version);
        documentRepository.save(document);

        return version;
    }
}
