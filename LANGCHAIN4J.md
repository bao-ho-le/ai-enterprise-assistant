# LangChain4j trong AI Enterprise Assistant

Tài liệu gồm 2 chương:

- **Chương I** — Các class, method, annotation của LangChain4j mà backend đang dùng, mỗi thứ làm gì.
- **Chương II** — Các luồng chức năng AI chính: nên đọc file nào trước, hàm nào, theo thứ tự nào.

> **Cách đọc đường dẫn:** tên file đều bấm được. Thư mục gốc của code Java là
> `backend/src/main/java/com/enterprise/aiassistant/backend/`.

---

# Chương I. Các thành phần LangChain4j đang dùng

## I.0 Thư viện

| Thư viện                       | Phiên bản     | Chứa gì                                                      |
| ------------------------------ | ------------- | ------------------------------------------------------------ |
| `langchain4j-core`             | 1.22.0        | Phần lõi: model, tin nhắn, prompt, kho vector, RAG, listener |
| `langchain4j`                  | 1.22.0        | AI Services (`AiServices`, `@UserMessage`, `@V`, `Result`)   |
| `langchain4j-google-ai-gemini` | 1.22.0        | Kết nối Gemini (chat và embedding)                           |
| `langchain4j-qdrant`           | 1.22.0-beta32 | Kho vector trên Qdrant (`QdrantEmbeddingStore`)              |

---

## I.1 Gọi model AI (Gemini)

### 1. `ChatModel` — interface · `langchain4j-core`

- **Chức năng:** đại diện cho một model chat (LLM). Gửi tin nhắn đi, nhận câu trả lời về.
- **Method dùng:**
  - `chat(UserMessage)` → `ChatResponse`: gửi một tin nhắn, nhận phản hồi đầy đủ (nội dung + token).
  - `chat(String)` → `String`: được `CompressingQueryTransformer` dùng bên trong.
- **Dùng ở:** `GenerationServiceImpl`, `SummaryChatServiceImpl`, `QuestionRewriter`, `DocumentQAConfig`.

### 2. `GoogleAiGeminiChatModel` — class · `langchain4j-google-ai-gemini`

- **Chức năng:** bản cài đặt `ChatModel` để gọi Gemini API.
- **Builder dùng:**
  - `apiKey(...)`, `modelName(...)`: khoá API và tên model.
  - `logRequestsAndResponses(...)`: in request/response ra log khi cần debug.
  - `supportedCapabilities(RESPONSE_FORMAT_JSON_SCHEMA)`: cho phép gửi JSON schema.
  - `listeners(...)`: gắn listener để ghi usage log.
- **Dùng ở:** `GeminiConfig.geminiChatModel()`.

### 3. `Capability` — enum · `langchain4j-core`

- **Chức năng:** khai báo khả năng của model. `RESPONSE_FORMAT_JSON_SCHEMA` = model trả được JSON đúng theo schema.
  Nhờ vậy, AI Service có method trả enum sẽ gửi schema kèm request, Gemini chỉ trả được giá trị hợp lệ.
- **Dùng ở:** `GeminiConfig`.

### 4. `ChatResponse` — class · `langchain4j-core`

- **Chức năng:** kết quả của một lượt chat.
- **Method dùng:** `aiMessage().text()` (câu trả lời), `tokenUsage()` (số token), `metadata().modelName()` (tên model thực tế).
- **Dùng ở:** `GenerationServiceImpl`, `SummaryChatServiceImpl`, `AIUsageListener`.

### 5. `TokenUsage` — class · `langchain4j-core`

- **Chức năng:** số token đã dùng. `inputTokenCount()`, `outputTokenCount()`.
- **Dùng ở:** `AIUsageListener`.

### 6. `InvalidRequestException` — exception · `langchain4j-core`

- **Chức năng:** lỗi 400 do Gemini trả về. `LLMHelper.call()` bắt lỗi này; nếu nội dung có "token count" thì báo `LLM_INPUT_TOO_LARGE` (prompt quá dài).
- **Dùng ở:** `LLMHelper`.

### 7. `EmbeddingModel` — interface · `langchain4j-core`

- **Chức năng:** biến văn bản thành vector số (embedding) để so sánh độ giống nhau.
- **Method dùng:**
  - `embed(String)`: tạo 1 vector (câu tìm kiếm, câu hỏi).
  - `embedAll(List<TextSegment>)`: tạo nhiều vector một lần (các đoạn tài liệu).
  - `modelName()`: tên model.
- **Dùng ở:** `DocumentProcessingService`, `SemanticSearchServiceImpl`, `DocumentQAConfig`.

### 8. `GoogleAiEmbeddingModel` — class · `langchain4j-google-ai-gemini`

- **Chức năng:** bản cài đặt `EmbeddingModel` gọi `gemini-embedding-001` (vector 3072 chiều). Tự chia tối đa 100 đoạn mỗi request.
- **Builder dùng:** `apiKey(...)`, `modelName(...)`, `listeners(...)`.
- **Dùng ở:** `GeminiEmbeddingModelConfig.embeddingModel()`.

### 9. `Embedding` — class · `langchain4j-core`

- **Chức năng:** một vector số thực, kết quả của `EmbeddingModel`.
- **Dùng ở:** `DocumentProcessingService`, `SemanticSearchServiceImpl`.

---

## I.2 Tin nhắn và prompt

### 10. `UserMessage` (package `dev.langchain4j.data.message`) — class · `langchain4j-core`

- **Chức năng:** tin nhắn phía người dùng gửi cho model.
- **Method dùng:** `UserMessage.from(text)`, `singleText()` (lấy nội dung chữ).
- **Dùng ở:** `GenerationServiceImpl`, `SummaryChatServiceImpl`, `QuestionRewriter`, `DocumentQAConfig`.
- ⚠ Khác với annotation `@UserMessage` ở mục 14 (cùng tên, khác package).

### 11. `ChatMessage` — interface · `langchain4j-core`

- **Chức năng:** kiểu chung cho mọi loại tin nhắn (user, AI, system).
- **Dùng ở:** `QuestionRewriter.format(List<ChatMessage>)`.

### 12. `PromptTemplate` — class · `langchain4j-core`

- **Chức năng:** mẫu văn bản có chỗ trống `{{tên}}`, điền giá trị vào để ra prompt hoàn chỉnh.
  Bản 1.22 điền **một lượt**: nếu giá trị (ví dụ nội dung tài liệu) có chứa `{{...}}` thì vẫn giữ nguyên.
- **Method dùng:** `PromptTemplate.from(text)`, `apply(Map)` → `Prompt`, `.text()`.
- **Dùng ở:** `PromptBuilderService` (prompt email, báo cáo, tóm tắt, biểu mẫu), `QuestionRewriter` (qua lớp cha).

---

## I.3 AI Services — khai báo interface, LangChain4j tự viết phần thực thi

### 13. `AiServices` — class · `langchain4j`

- **Chức năng:** tạo phần thực thi cho một interface Java. Khi gọi method, LangChain4j tự đọc file prompt, điền biến, gọi `ChatModel`, rồi đổi kết quả sang kiểu trả về của method.
- **Method dùng:**
  - `AiServices.create(ChatAssistant.class, chatModel)`: AI Service đơn giản.
  - `AiServices.builder(DocumentQAAssistant.class).chatModel(...).retrievalAugmentor(...).build()`: AI Service có gắn RAG.
- **Dùng ở:** `GeminiConfig`, `DocumentQAConfig`.

### 14. `@UserMessage(fromResource = "prompts/x.txt")` — annotation · `langchain4j`

- **Chức năng:** đặt trên method của AI Service. Nội dung file là prompt sẽ gửi cho model.
- **Dùng ở:** 4 method của `ChatAssistant`, method `answer` của `DocumentQAAssistant`.

### 15. `@V("tên")` — annotation · `langchain4j`

- **Chức năng:** đặt trên tham số của method. Giá trị tham số được điền vào chỗ `{{tên}}` trong prompt.
- **Dùng ở:** `ChatAssistant`, `DocumentQAAssistant`.

### 16. `Result<T>` — class · `langchain4j`

- **Chức năng:** kiểu trả về của AI Service khi cần thêm thông tin ngoài câu trả lời.
- **Method dùng:** `content()` (câu trả lời), `tokenUsage()`, `sources()` (các đoạn tài liệu RAG đã dùng để trả lời).
- **Dùng ở:** `DocumentQAAssistant.answer`, `DocumentQAServiceImpl`.

### 17. Tự đổi kết quả sang enum — tính năng của `langchain4j`

- **Chức năng:** method của AI Service khai báo trả về `Intent` (enum). LangChain4j tự gửi JSON schema chứa 4 giá trị, rồi tự đổi `{"value":"SUMMARY"}` thành `Intent.SUMMARY`.
- **Dùng ở:** `ChatAssistant.classifyIntent`.

### 18. `InvocationParameters` — class · `langchain4j-core`

- **Chức năng:** túi tham số đi kèm **một lần gọi** AI Service. Các bước RAG bên trong đọc lại được qua `query.metadata().invocationParameters()`.
- **Method dùng:** `InvocationParameters.from(Map)`, `get(key)`.
- **Dùng ở:** `DocumentQAServiceImpl` (truyền câu hỏi và danh sách tài liệu đính kèm), `DocumentQAConfig` (đọc ra).

---

## I.4 Dữ liệu tài liệu

### 19. `TextSegment` — class · `langchain4j-core`

- **Chức năng:** một đoạn văn bản kèm metadata. Đây là đơn vị được tạo vector và lưu vào Qdrant.
- **Method dùng:** `TextSegment.from(text, metadata)`, `text()`, `metadata()`.
- **Dùng ở:** chunking (`ChunkingMapper`, `ChunkingService`, các `ChunkingStrategy`), `ProcessingMapper`, `DocumentProcessingService`, `VectorStoreMapper`, `SearchMapper`.

### 20. `Metadata` (package `dev.langchain4j.data.document`) — class · `langchain4j-core`

- **Chức năng:** các cặp key → value đi theo `TextSegment` (chunkId, documentId, số trang…). Khi lưu Qdrant, metadata trở thành **payload** của point.
- **Method dùng:** `put(key, value)`, `getLong(key)`, `getInteger(key)`.
- **Dùng ở:** `ChunkingMapper`, `ProcessingMapper`, `VectorStoreMapper`.

---

## I.5 Kho vector (Embedding Store)

### 21. `EmbeddingStore<TextSegment>` — interface · `langchain4j-core`

- **Chức năng:** kho lưu vector kèm đoạn văn, cho phép tìm đoạn giống nhất.
- **Method dùng:**
  - `addAll(ids, embeddings, segments)`: ghi (hoặc ghi đè) nhiều point.
  - `search(EmbeddingSearchRequest).matches()`: tìm kiếm.
- **Dùng ở:** `QdrantConfig` (tạo bean), `DocumentProcessingService`, `SemanticSearchServiceImpl`, `DocumentQAConfig`.

### 22. `QdrantEmbeddingStore` — class · `langchain4j-qdrant`

- **Chức năng:** bản cài đặt `EmbeddingStore` chạy trên Qdrant.
- **Builder dùng:** `client(qdrantClient)`, `collectionName(...)`, `payloadTextKey("content")` (nội dung đoạn văn nằm ở payload key `content`).
- **Dùng ở:** `QdrantConfig.embeddingStore()`.

### 23. `EmbeddingSearchRequest` — class · `langchain4j-core`

- **Chức năng:** mô tả một yêu cầu tìm kiếm.
- **Builder dùng:** `queryEmbedding(vector)`, `maxResults(n)`, `minScore(x)`, `filter(f)`.
- **Dùng ở:** `SemanticSearchServiceImpl` (Document QA dùng gián tiếp qua retriever, mục 30).

### 24. `EmbeddingMatch<TextSegment>` — class · `langchain4j-core`

- **Chức năng:** một kết quả tìm được. `score()` (điểm), `embedded()` (đoạn văn + metadata).
- **Dùng ở:** `VectorStoreMapper`, `SearchMapper`, `SemanticSearchServiceImpl`.

### 25. `MetadataFilterBuilder.metadataKey(key)` — static method · `langchain4j-core`

- **Chức năng:** tạo bộ lọc theo payload; Qdrant lọc ngay khi tìm.
- **Method dùng:** `.isEqualTo(value)`, `.isIn(list)`.
- **Dùng ở:** `SemanticSearchServiceImpl` (lọc theo `documentId`), `DocumentQAConfig` (lọc theo `documentVersionId` của tài liệu đính kèm).

### 26. `RelevanceScore`, `CosineSimilarity` — class · `langchain4j-core`

- **Chức năng:** đổi giữa 2 thang điểm. LangChain4j dùng **relevance** = (cosine + 1) / 2, còn hệ thống lưu và hiển thị **cosine**.
- **Method dùng:**
  - `RelevanceScore.fromCosineSimilarity(0.65)` = 0.825: ngưỡng tìm kiếm.
  - `CosineSimilarity.fromRelevanceScore(score)`: đổi điểm trả về sang cosine.
- **Dùng ở:** `VectorStoreHelper.minScore()`, `VectorStoreMapper.cosineScore()`.

---

## I.6 RAG — tìm tài liệu rồi đưa vào prompt

### 27. `DefaultRetrievalAugmentor` — class · `langchain4j-core`

- **Chức năng:** "dây chuyền" RAG, tự chạy mỗi lần gọi AI Service có gắn nó. Thứ tự: biến đổi câu hỏi → chọn nguồn tìm → tìm → ghép kết quả vào prompt.
- **Builder dùng:** `queryTransformer(...)`, `queryRouter(...)`, `contentInjector(...)`.
- **Dùng ở:** `DocumentQAConfig`.

### 28. `QueryTransformer` — interface · `langchain4j-core` (dùng dạng lambda)

- **Chức năng:** biến đổi câu đem đi tìm.
- **Trong dự án:** thay "toàn bộ prompt" bằng đúng câu hỏi lấy từ `InvocationParameters`.
- **Dùng ở:** `DocumentQAConfig`.

### 29. `QueryRouter` — interface · `langchain4j-core` (dùng dạng lambda)

- **Chức năng:** chọn nguồn để tìm.
- **Trong dự án:** không có tài liệu nào đọc được thì không tìm.
- **Dùng ở:** `DocumentQAConfig`.

### 30. `ContentRetriever` / `EmbeddingStoreContentRetriever` — interface / class · `langchain4j-core`

- **Chức năng:** lấy các đoạn liên quan: tạo vector cho câu hỏi rồi tìm trong `EmbeddingStore`.
- **Builder dùng:** `embeddingStore(...)`, `embeddingModel(...)`, `maxResults(5)`, `minScore(...)`, `dynamicFilter(query -> filter)` (bộ lọc tính riêng cho mỗi câu hỏi).
- **Dùng ở:** `DocumentQAConfig`.

### 31. `ContentInjector` — interface · `langchain4j-core` (dùng dạng lambda)

- **Chức năng:** ghép các đoạn tìm được vào tin nhắn gửi model.
- **Trong dự án:** tự viết (không dùng bản mặc định) để prompt giữ đúng định dạng cũ.
- **Dùng ở:** `DocumentQAConfig`.

### 32. `Query`, `Metadata` (package `dev.langchain4j.rag.query`) — class · `langchain4j-core`

- **Chức năng:** câu đem đi tìm, kèm thông tin đi theo (tin nhắn gốc, lịch sử chat, `InvocationParameters`).
- **Method dùng:** `Query.from(text, metadata)`, `Metadata.from(message, memoryId, chatMemory)`, `query.metadata().invocationParameters()`.
- **Dùng ở:** `DocumentQAConfig`, `QuestionRewriter`.

### 33. `Content`, `ContentMetadata` — · `langchain4j-core`

- **Chức năng:** một đoạn tìm được trong RAG. `textSegment()` (đoạn văn + metadata), `metadata().get(ContentMetadata.SCORE)` (điểm).
- **Dùng ở:** `VectorStoreMapper`, `DocumentQAServiceImpl`.

### 34. `CompressingQueryTransformer` — class · `langchain4j-core`

- **Chức năng:** dựa vào lịch sử chat, viết lại câu hỏi phụ thuộc ngữ cảnh ("nó", "cái đó") thành câu hỏi độc lập.
- **Method dùng:** `transform(Query)`; `format(List<ChatMessage>)` được dự án override để đưa memory 2 tầng vào.
- **Dùng ở:** `QuestionRewriter extends CompressingQueryTransformer`.

---

## I.7 Listener — theo dõi mỗi lượt gọi model

### 35. `ChatModelListener` — interface · `langchain4j-core`

- **Chức năng:** LangChain4j tự gọi listener sau **mỗi** lượt `ChatModel.chat`, dù lời gọi đi từ đâu. Listener chạy cùng thread; nếu listener lỗi thì LangChain4j bỏ qua, lượt gọi AI vẫn bình thường.
- **Method dùng:**
  - `onResponse(ChatModelResponseContext)`: lấy `chatResponse()` (token, tên model).
  - `onError(ChatModelErrorContext)`: lấy `error()`, `chatRequest().modelName()`.
- **Dùng ở:** `AIUsageListener`.

### 36. `EmbeddingModelListener` — interface · `langchain4j-core`

- **Chức năng:** giống mục 35 nhưng cho mỗi lượt embedding.
- **Method dùng:** `onResponse(EmbeddingModelResponseContext)` (`textSegments()`, `embeddingModel().modelName()`), `onError(EmbeddingModelErrorContext)`.
- **Dùng ở:** `AIUsageListener`.

---

# Chương II. Luồng các chức năng AI

Mỗi luồng có: **chức năng là gì** → **danh sách file theo thứ tự nên đọc** (file nào, hàm nào, xem gì ở đó).
Những dòng 🔹 là chỗ LangChain4j làm việc.

| Luồng                                                       | Nội dung                                         |
| ----------------------------------------------------------- | ------------------------------------------------ |
| [II.1](#ii1-khởi-tạo--nên-đọc-trước-tiên)                   | Khởi tạo: các file cấu hình nên đọc trước tiên   |
| [II.2](#ii2-upload-và-index-tài-liệu)                       | Upload và index tài liệu vào Qdrant              |
| [II.3](#ii3-semantic-search)                                | Semantic search                                  |
| [II.4](#ii4-gửi-tin-nhắn-chat-và-phân-loại-intent)          | Gửi tin nhắn chat và phân loại intent            |
| [II.5](#ii5-hỏi-đáp-tài-liệu-document-qa)                   | Hỏi đáp tài liệu (Document QA, RAG)              |
| [II.6](#ii6-chat-thường-tóm-tắt-hội-thoại-tóm-tắt-tài-liệu) | Chat thường, tóm tắt hội thoại, tóm tắt tài liệu |
| [II.7](#ii7-memory-2-tầng-và-nén-memory)                    | Memory 2 tầng và nén memory                      |
| [II.8](#ii8-generation-email-báo-cáo-tóm-tắt-biểu-mẫu)      | Generation: email, báo cáo, tóm tắt, biểu mẫu    |
| [II.9](#ii9-ghi-usage-log)                                  | Ghi usage log                                    |

---

## II.1 Khởi tạo — nên đọc trước tiên

**Chức năng:** lúc app khởi động, tạo sẵn các "công cụ" mà mọi luồng AI dùng chung: model chat, model embedding, kho vector, các AI Service.

**Đọc theo thứ tự:**

1. [`GeminiConfig.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/llm/config/GeminiConfig.java)
   - `geminiChatModel()`: tạo model chat Gemini.
     - 🔹 `GoogleAiGeminiChatModel.builder()` với JSON schema và listener ghi usage log.
   - `chatAssistant()`: kiểm tra JVM chạy UTF-8, rồi tạo AI Service.
     - 🔹 `AiServices.create(ChatAssistant.class, chatModel)`.
2. [`ChatAssistant.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/llm/service/ChatAssistant.java) — xem 4 method và annotation `@UserMessage(fromResource)` / `@V`. Chỉ có interface, không có code thực thi.
3. [`GeminiEmbeddingModelConfig.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/embedding/config/GeminiEmbeddingModelConfig.java)
   - `embeddingModel()`: tạo model embedding.
     - 🔹 `GoogleAiEmbeddingModel.builder()` + listener.
4. [`QdrantConfig.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/vectorstore/config/QdrantConfig.java)
   - `embeddingStore()`: tạo kho vector.
     - 🔹 `QdrantEmbeddingStore.builder()`.
5. [`DocumentQAConfig.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/knowledge/qa/config/DocumentQAConfig.java)
   - `documentQAAssistant()`: lắp dây chuyền RAG. Chi tiết đọc ở II.5.

---

## II.2 Upload và index tài liệu

**Chức năng:** khi người dùng upload tài liệu, hệ thống chạy nền:
trích văn bản → chia thành các đoạn → tạo vector cho từng đoạn → lưu vào Qdrant để sau này tìm kiếm.

**Đọc theo thứ tự:**

1. [`DocumentServiceImpl.java`](backend/src/main/java/com/enterprise/aiassistant/backend/document/service/DocumentServiceImpl.java)
   - Tìm `publishEvent(new DocumentVersionCreatedEvent(...))`: upload xong thì phát sự kiện. Upload nhiều file thì dùng `DocumentVersionCreatedBatchEvent`.
2. [`DocumentProcessingWorker.java`](backend/src/main/java/com/enterprise/aiassistant/backend/processing/orchestration/worker/DocumentProcessingWorker.java)
   - `onDocumentVersionCreated()`: nhận sự kiện, chạy nền (`@Async`) sau khi lưu DB xong → `submit()`.
3. [`DocumentProcessingRetryExecutor.java`](backend/src/main/java/com/enterprise/aiassistant/backend/processing/orchestration/worker/DocumentProcessingRetryExecutor.java)
   - `processWithRetry()`: nếu gặp lỗi file, xử lý, embedding hoặc kho vector thì chạy lại, tổng cộng tối đa 3 lần.
4. [`DocumentProcessingService.java`](backend/src/main/java/com/enterprise/aiassistant/backend/processing/orchestration/service/DocumentProcessingService.java) → `process()`: **hàm chính, đọc lần lượt các bước:**
   - **Bước 1** — trích văn bản (`textExtractionService.extract`). Bước này không dùng LangChain4j.
   - **Bước 2** — chia đoạn: `chunkingService.chunk(...)`. Đọc tiếp:
     - [`DocumentChunkingStrategy.java`](backend/src/main/java/com/enterprise/aiassistant/backend/processing/chunking/strategy/DocumentChunkingStrategy.java) `chunk()`: thuật toán chia đoạn.
     - [`ChunkingMapper.java`](backend/src/main/java/com/enterprise/aiassistant/backend/processing/chunking/mapper/ChunkingMapper.java) `toTextSegment()`
       - 🔹 Tạo `TextSegment` + `Metadata` (số thứ tự đoạn, trang, vị trí).
   - **Lưu từng đoạn vào DB:**
     - [`ProcessingMapper.java`](backend/src/main/java/com/enterprise/aiassistant/backend/processing/orchestration/mapper/ProcessingMapper.java) `toDocumentChunk()`: đọc `TextSegment` để tạo entity.
     - [`VectorStoreMapper.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/vectorstore/mapper/VectorStoreMapper.java) `addChunkIdentity()`: thêm `chunkId`, `documentId`, `documentVersionId` vào metadata.
   - **Bước 3** — tạo vector, mỗi lô 100 đoạn: `embeddingHelper.call(() -> embeddingModel.embedAll(batch))`
     - 🔹 `EmbeddingModel.embedAll` gọi Gemini; listener tự ghi 1 dòng usage log cho mỗi lô.
   - **Lưu vào Qdrant:** `vectorStoreHelper.call(..., () -> embeddingStore.addAll(ids, embeddings, segments))`
     - 🔹 `EmbeddingStore.addAll` ghi vào Qdrant.
     - ID của point lấy từ `VectorStoreMapper.pointId()` (UUID sinh từ chunkId, nên retry sẽ ghi đè đúng point cũ).
5. [`EmbeddingHelper.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/embedding/helper/EmbeddingHelper.java) `call()` và [`VectorStoreHelper.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/vectorstore/helper/VectorStoreHelper.java) `call()`
   - Đổi lỗi của LangChain4j về đúng loại lỗi mà bước 3 sẽ retry.

---

## II.3 Semantic search

**Chức năng:** người dùng gõ từ khoá; hệ thống tìm các đoạn tài liệu có **nghĩa** gần nhất (không cần trùng chữ).

**Đọc theo thứ tự:**

1. [`SemanticSearchController.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/knowledge/search/controller/SemanticSearchController.java)
   - API `GET /api/v1/search/semantic`.
2. [`SemanticSearchServiceImpl.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/knowledge/search/service/SemanticSearchServiceImpl.java) → `search()`:
   - Kiểm tra request và quyền; lấy số kết quả `topK` (mặc định 10, tối đa 50).
   - Tạo vector cho từ khoá: `embeddingModel.embed(keyword)`, nằm trong `AIUsageContext(SEMANTIC_SEARCH)`.
     - 🔹 Gọi Gemini embedding; listener ghi usage log.
   - Tạo yêu cầu tìm: `EmbeddingSearchRequest.builder()...`
     - 🔹 Số kết quả, ngưỡng điểm `minScore`, lọc theo tài liệu nếu có (`metadataKey(DOCUMENT_ID).isEqualTo(...)`).
   - Tìm: `embeddingStore.search(request).matches()`
     - 🔹 Qdrant trả về danh sách `EmbeddingMatch`.
   - `fetchActiveDocuments()` + `isCurrentVersionHit()`: bỏ kết quả của tài liệu đã xoá, không có quyền đọc, hoặc không phải phiên bản hiện hành.
3. [`VectorStoreHelper.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/vectorstore/helper/VectorStoreHelper.java) → `minScore()`
   - Ngưỡng cosine 0.65 đổi sang thang relevance của LangChain4j là 0.825.
4. [`SearchMapper.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/knowledge/search/mapper/SearchMapper.java) → `toSemanticSearchResults()`
   - Đổi kết quả sang dạng trả về client, dùng `VectorStoreMapper` để đọc metadata và `cosineScore()` để đổi điểm về cosine.

---

## II.4 Gửi tin nhắn chat và phân loại intent

**Chức năng:** người dùng gửi tin nhắn trong conversation. Gemini xác định tin nhắn thuộc loại nào (intent), rồi hệ thống chuyển sang luồng xử lý phù hợp:

| Intent                 | Ý nghĩa                           | Xử lý ở |
| ---------------------- | --------------------------------- | ------- |
| `DOCUMENT_QA`          | Hỏi về nội dung tài liệu đính kèm | II.5    |
| `SUMMARY`              | Tóm tắt tài liệu đính kèm         | II.6    |
| `GENERAL_CHAT`         | Chat thường, không cần tài liệu   | II.6    |
| `CONVERSATION_SUMMARY` | Tóm tắt chính cuộc hội thoại      | II.6    |

**Đọc theo thứ tự:**

1. [`AIMessageController.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/chat/message/controller/AIMessageController.java) → `sendMessage()`
   - API `POST /api/v1/ai-conversations/{id}/messages`.
2. [`AIMessageServiceImpl.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/chat/message/service/AIMessageServiceImpl.java) → `sendMessage()`:
   - Kiểm tra request, conversation và chủ sở hữu (`getConversationOrThrow()`); lưu tin nhắn người dùng.
   - Mở `AIUsageContext` cho cả lượt chat, để mọi lời gọi AI bên trong đều được ghi usage log.
   - Gọi `answerByIntent()` (bước 3), rồi cập nhật memory (`appendTurn`, xem II.7).
3. Cùng file → `answerByIntent()`
   - Gọi phân loại intent, kiểm tra quyền theo intent (`requireIntentPermission()`), rồi rẽ nhánh `switch`.
4. [`IntentClassifierImpl.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/chat/intent/service/IntentClassifierImpl.java) → `classify()`
   - `llmHelper.call(() -> chatAssistant.classifyIntent(message))`; lỗi bất kỳ thì trả `GENERAL_CHAT`.
5. [`ChatAssistant.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/llm/service/ChatAssistant.java) → `classifyIntent()` cùng file [`intent-classification.txt`](backend/src/main/resources/prompts/intent-classification.txt)
   - 🔹 AI Service điền `{{message}}` vào prompt, gửi kèm JSON schema (4 giá trị enum), Gemini trả `{"value":"..."}`, LangChain4j tự đổi sang enum `Intent`.
6. [`LLMHelper.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/llm/helper/LLMHelper.java) → `call()`
   - Mọi lời gọi Gemini đều đi qua đây: lỗi của LangChain4j đổi thành `LLMException`.

---

## II.5 Hỏi đáp tài liệu (Document QA)

**Chức năng:** trả lời câu hỏi dựa trên tài liệu đính kèm, kèm nguồn trích dẫn. Gồm 2 phần:

- **(A) Viết lại câu hỏi:** "Nó có hạn mức bao nhiêu?" → "Gói Bạc có hạn mức bao nhiêu?" (dựa vào memory).
- **(B) RAG:** tìm đoạn tài liệu liên quan → đưa vào prompt → Gemini trả lời.

**Bắt đầu từ:** `AIMessageServiceImpl.answerByIntent()`, nhánh `case DOCUMENT_QA` → gọi `questionRewriter.rewrite(...)` rồi `documentQAService.answer(...)`.

### (A) Viết lại câu hỏi — đọc theo thứ tự

1. [`QuestionRewriter.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/knowledge/qa/service/QuestionRewriter.java)
   - **Constructor:** truyền `ChatModel` và prompt [`question-rewrite.txt`](backend/src/main/resources/prompts/question-rewrite.txt) cho lớp cha `CompressingQueryTransformer`.
   - **`rewrite()`:**
     - Lấy memory 2 tầng (`conversationMemoryService.buildMemoryContext`).
     - 🔹 Gói câu hỏi và memory thành `Query`.
     - Gọi `transform(query)` của lớp cha.
       - 🔹 Lớp cha điền `{{chatMemory}}` và `{{query}}` vào prompt rồi gọi Gemini.
   - **`format()`:** đưa nguyên văn memory 2 tầng vào `{{chatMemory}}`.
   - **`normalize()`:** bỏ dấu ngoặc kép bao ngoài; kết quả rỗng hoặc lỗi thì giữ câu gốc.

### (B) Trả lời bằng RAG — đọc theo thứ tự

1. [`DocumentQAServiceImpl.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/knowledge/qa/service/DocumentQAServiceImpl.java) → `answer()`:
   - Lấy tài liệu đính kèm, bỏ tài liệu người dùng không còn quyền đọc → danh sách `attachedVersionIds`.
   - Lấy memory 2 tầng.
   - 🔹 Tạo `InvocationParameters` chứa câu hỏi và `attachedVersionIds`.
   - Gọi `documentQAAssistant.answer(question, memory, parameters)` (bước 2).
   - Lưu câu trả lời; lấy `answer.sources()` để lưu nguồn trích dẫn (`toMessageSource()`).
2. [`DocumentQAAssistant.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/knowledge/qa/service/DocumentQAAssistant.java) cùng file [`document-qa.txt`](backend/src/main/resources/prompts/document-qa.txt)
   - Interface AI Service; prompt kết thúc ở dòng `Document excerpts:` để đoạn trích được nối vào sau.
3. [`DocumentQAConfig.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/knowledge/qa/config/DocumentQAConfig.java) → `documentQAAssistant()`: **đây là chỗ LangChain4j làm phần lớn công việc.** Đọc theo thứ tự chạy thật (không phải thứ tự dòng code):
   - 🔹 AI Service điền `{{question}}`, `{{conversationMemory}}` vào `document-qa.txt`.
   - 🔹 `queryTransformer`: thay câu tìm kiếm (mặc định là cả prompt) bằng đúng câu hỏi.
   - 🔹 `queryRouter`: không có tài liệu đọc được thì không tìm.
   - 🔹 `contentRetriever` (`EmbeddingStoreContentRetriever`):
     - tạo vector cho câu hỏi (Gemini embedding, listener ghi log);
     - tìm trong Qdrant: tối đa 5 đoạn, điểm ≥ ngưỡng, chỉ trong tài liệu đính kèm (`isIn`).
   - 🔹 `contentInjector`: nối các đoạn tìm được (ngăn cách `---`) vào cuối prompt.
   - 🔹 Gọi Gemini trả lời (listener ghi log) → trả về `Result` gồm câu trả lời, token và `sources`.
4. [`PromptBuilderService.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/prompt/service/PromptBuilderService.java) → `buildDocumentExcerpts()`
   - Cách ghép đoạn trích; không có đoạn nào thì ghi "No relevant passages were found.".
5. [`VectorStoreMapper.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/vectorstore/mapper/VectorStoreMapper.java) → `chunkId(Content)`, `documentVersionId(Content)`, `cosineScore(Content)`
   - Đọc từng nguồn để lưu bằng chứng trích dẫn.

> Một câu hỏi về tài liệu gọi Gemini **3 lần** (intent, viết lại, trả lời) và embedding **1 lần**.

---

## II.6 Chat thường, tóm tắt hội thoại, tóm tắt tài liệu

**Chức năng:** 3 nhánh còn lại của `answerByIntent()`. Câu trả lời được lưu bằng `AIMessageServiceImpl.saveAssistantMessage()`.

**Chat thường (`GENERAL_CHAT`)** — đọc theo thứ tự:

1. [`GeneralChatServiceImpl.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/chat/handler/service/GeneralChatServiceImpl.java) → `answer()`
2. [`ChatAssistant.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/llm/service/ChatAssistant.java) → `generalChat()` + [`general-chat.txt`](backend/src/main/resources/prompts/general-chat.txt)
   - 🔹 AI Service gọi Gemini.

**Tóm tắt hội thoại (`CONVERSATION_SUMMARY`)** — đọc theo thứ tự:

1. [`ConversationSummaryChatServiceImpl.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/chat/handler/service/ConversationSummaryChatServiceImpl.java) → `summarize()`
   - Lấy memory 2 tầng.
2. [`ChatAssistant.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/llm/service/ChatAssistant.java) → `summarizeConversation()` + [`conversation-summary.txt`](backend/src/main/resources/prompts/conversation-summary.txt)
   - 🔹 AI Service gọi Gemini.

**Tóm tắt tài liệu (`SUMMARY`)** — đọc theo thứ tự:

1. [`SummaryChatServiceImpl.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/chat/handler/service/SummaryChatServiceImpl.java) → `summarize()`
   - Lấy toàn văn tài liệu (`documentContextService.buildContext`).
   - Tạo prompt: `promptBuilderService.buildSummaryPrompt(...)`.
   - Gửi: `chatModel.chat(UserMessage.from(prompt))`.
2. [`PromptBuilderService.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/prompt/service/PromptBuilderService.java) → `buildSummaryPrompt()` + [`summary.txt`](backend/src/main/resources/prompts/summary.txt)
   - 🔹 `PromptTemplate.apply(...)` điền prompt.
   - Prompt đã điền sẵn nên **gửi thẳng qua `ChatModel`**, không qua AI Service: nếu đưa vào AI Service, nội dung tài liệu chứa `{{...}}` sẽ bị coi là biến và gây lỗi.

---

## II.7 Memory 2 tầng và nén memory

**Chức năng:** ghi nhớ cuộc hội thoại để Gemini hiểu ngữ cảnh. Memory gồm 2 phần:

- `pendingContext`: các lượt chat mới, ghi nguyên văn.
- `summarizedContext`: bản tóm tắt các lượt cũ.

Khi tổng ≥ 16.000 ký tự, Gemini nén lại thành bản tóm tắt khoảng 6.000 ký tự.
**Phần này không dùng `ChatMemory` của LangChain4j**; memory được đưa vào prompt dưới dạng text.

**Đọc theo thứ tự:**

1. [`AIMessageServiceImpl.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/chat/message/service/AIMessageServiceImpl.java) → `sendMessage()`
   - Có câu trả lời thì gọi `conversationMemoryService.appendTurn(...)`.
2. [`ConversationMemoryServiceImpl.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/chat/memory/service/ConversationMemoryServiceImpl.java):
   - `buildMemoryContext()`: đọc memory thành text, dùng cho intent, viết lại câu hỏi, Document QA, tóm tắt hội thoại.
   - `appendTurn()`: thêm lượt chat mới, đếm ký tự, đủ ngưỡng thì gọi `compress()`.
   - `compress()`: `llmHelper.call(() -> chatAssistant.compressMemory(...))`.
     - 🔹 AI Service + [`conversation-memory-summary.txt`](backend/src/main/resources/prompts/conversation-memory-summary.txt).
     - Nén lỗi thì giữ nguyên memory.
3. [`ConversationMemoryHelper.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/chat/memory/helper/ConversationMemoryHelper.java)
   - `appendTurn()`, `needsSummarization()` (ngưỡng 16.000), `buildContext()` (ghép 2 phần thành text).

---

## II.8 Generation (email, báo cáo, tóm tắt, biểu mẫu)

**Chức năng:** người dùng điền form (mục đích, người nhận, giọng văn…); Gemini viết nội dung hoàn chỉnh.

**Đọc theo thứ tự:**

1. [`AIConversationController.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/chat/conversation/controller/AIConversationController.java) → `startGenerationConversation()`
   - API `POST /api/v1/ai-conversations/generation/start`. Tạo conversation rồi gọi `generate()`.
   - Cách khác: [`GenerationController.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/knowledge/generation/controller/GenerationController.java), API `POST /api/v1/ai-conversations/{id}/generate`.
2. [`GenerationServiceImpl.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/knowledge/generation/service/GenerationServiceImpl.java) → `generate()`:
   - Kiểm tra request, quyền, chủ sở hữu.
   - Chọn handler theo loại (`resolveHandler()`), gọi `handler.handle(...)` để có prompt.
   - Lưu `Generation` trạng thái `RUNNING`.
   - Trong `AIUsageContext(type, conversationId, generationId)`, gọi `chatModel.chat(UserMessage.from(prompt))`.
     - 🔹 Gọi Gemini; listener ghi log kèm `generationId`.
   - Lưu nội dung, đặt trạng thái `COMPLETED`. Lỗi thì đặt `FAILED` rồi ném lỗi.
3. [`EmailGenerationHandler.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/knowledge/generation/handler/EmailGenerationHandler.java) → `handle()`
   - Đọc input, gọi `promptBuilderService.buildEmailPrompt(...)`.
   - Report và Summary handler còn lấy thêm toàn văn tài liệu (`DocumentContextService.buildContext`).
4. [`PromptBuilderService.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/prompt/service/PromptBuilderService.java) → `buildEmailPrompt()` + [`email.txt`](backend/src/main/resources/prompts/email.txt)
   - 🔹 `PromptTemplate.apply(Map)` điền các trường form vào prompt.

---

## II.9 Ghi usage log

**Chức năng:** mỗi lần gọi Gemini (chat hoặc embedding) ghi 1 dòng vào bảng `ai_usage_logs`: loại, conversation, model, số token, thành công hay thất bại, người dùng. Trang Admin thống kê từ bảng này.

**Ý tưởng:** không có service nào tự ghi log. LangChain4j tự gọi listener sau mỗi lượt gọi model; service chỉ cần cho listener biết "đang ở luồng nào" bằng `AIUsageContext`.

**Đọc theo thứ tự:**

1. [`AIUsageContext.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/analytics/usage/listener/AIUsageContext.java)
   - `open(type, conversationId, generationId)`: ghi lại luồng hiện tại (ThreadLocal).
   - `current()`: listener đọc ra.
   - `close()`: tự dọn khi ra khỏi khối `try (...)`.
2. 4 chỗ mở context — tìm `AIUsageContext.open` trong:
   - `AIMessageServiceImpl.sendMessage()`: cả lượt chat (intent, viết lại, embedding, trả lời, nén memory).
   - `GenerationServiceImpl.generate()`: lượt gọi Gemini của generation.
   - `SemanticSearchServiceImpl.search()`: lượt embedding từ khoá.
   - `DocumentProcessingService.process()`: các lô embedding khi index.
3. [`GeminiConfig.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/llm/config/GeminiConfig.java) và [`GeminiEmbeddingModelConfig.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/infrastructure/embedding/config/GeminiEmbeddingModelConfig.java)
   - 🔹 Dòng `.listeners(List.of(aiUsageListener))`: gắn listener vào 2 model.
4. [`AIUsageListener.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/analytics/usage/listener/AIUsageListener.java):
   - 🔹 `onResponse(ChatModelResponseContext)`: lấy token và tên model từ phản hồi Gemini → `SUCCESS`.
   - 🔹 `onError(ChatModelErrorContext)`: lấy nội dung lỗi → `FAILED`.
   - 🔹 `onResponse` / `onError` cho embedding: Gemini không trả số token nên ước lượng bằng `embeddingInputTokens()` (ký tự / 4).
   - `logUsage()`: đọc `AIUsageContext.current()`, tạo request rồi gọi `aiUsageLogService.logAiUsage(...)`. Không có context thì chỉ in cảnh báo.
5. [`AIUsageLogServiceImpl.java`](backend/src/main/java/com/enterprise/aiassistant/backend/ai/analytics/usage/service/AIUsageLogServiceImpl.java):
   - `logAiUsage()`: kiểm tra dữ liệu, gắn người dùng và phòng ban đang đăng nhập (`attachOwner()`), phát sự kiện.
   - `onAIUsageLogEvent()`: chạy sau khi transaction của luồng commit xong, lưu dòng vào bảng `ai_usage_logs`.

> Ví dụ: một câu hỏi về tài liệu tạo **4 dòng** (intent, viết lại, embedding câu hỏi, trả lời); một tin nhắn chat thường tạo **2 dòng** (intent, trả lời).
