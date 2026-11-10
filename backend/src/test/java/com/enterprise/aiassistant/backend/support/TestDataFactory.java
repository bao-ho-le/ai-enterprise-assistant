package com.enterprise.aiassistant.backend.support;

import com.enterprise.aiassistant.backend.ai.analytics.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.analytics.usage.repository.AIUsageLogRepository;
import com.enterprise.aiassistant.backend.ai.chat.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.chat.conversation.repository.AIConversationRepository;
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
import com.enterprise.aiassistant.backend.document.entity.DocumentVersion;
import com.enterprise.aiassistant.backend.document.enums.DocumentType;
import com.enterprise.aiassistant.backend.document.enums.VersionStatus;
import com.enterprise.aiassistant.backend.document.repository.DocumentRepository;
import com.enterprise.aiassistant.backend.document.repository.DocumentVersionRepository;
import com.enterprise.aiassistant.backend.storage.entity.FileEntity;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Role;
import com.enterprise.aiassistant.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

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

    // Generated content không lưu chủ sở hữu trực tiếp: nó thuộc về chủ của conversation đã sinh ra nó
    // (qua bảng generations). Vì vậy phải tạo đủ chuỗi conversation → generated content → generation.
    public GeneratedContent createGeneratedContent(User owner, GeneratedDocumentType type, String title, String content) {
        AIConversation conversation = createConversation(owner, ConversationType.EMAIL_GENERATION);

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

    // ===================== Document =====================

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
