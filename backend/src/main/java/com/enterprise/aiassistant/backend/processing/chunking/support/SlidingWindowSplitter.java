package com.enterprise.aiassistant.backend.processing.chunking.support;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SlidingWindowSplitter {

    public static final int CHUNK_SIZE = 1000;

    public static final int OVERLAP_SIZE = 200;

    // Số cấp heading tối đa giữ trong breadcrumb (tính luôn cấp hiện tại), tránh
    // breadcrumb quá dài khi tài liệu lồng nhiều cấp heading.
    public static final int MAX_HEADING_BREADCRUMB_DEPTH = 3;

    public List<String> split(String text) {

        List<String> pieces = new ArrayList<>();

        // text rỗng thì return []
        if (text == null || text.isEmpty()) {
            return pieces;
        }

        int start = 0;

        // Lặp cho đến khi đọc hết
        while (start < text.length()) {

            // tránh vượt khỏi độ dài của text ban đầu
            int end = Math.min(
                    start + CHUNK_SIZE,
                    text.length()
            );

            pieces.add(text.substring(start, end));

            // Nếu đã tới cuối text thì dừng
            if (end == text.length()) {
                break;
            }

            // Lùi lại overlap
            start = end - OVERLAP_SIZE;
        }

        return pieces;
    }
}
