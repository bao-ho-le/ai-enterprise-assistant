package com.enterprise.aiassistant.backend.document.enums;

public enum ExtractionMethod {
    DIRECT_TEXT,      // Đọc trực tiếp từ file (PDF text, DOCX, XLSX...)
    OCR,              // OCR từ ảnh hoặc PDF scan
    MANUAL,           // Người dùng nhập tay
    AI_ENHANCED       // Sau này nếu AI xử lý bổ sung
}
