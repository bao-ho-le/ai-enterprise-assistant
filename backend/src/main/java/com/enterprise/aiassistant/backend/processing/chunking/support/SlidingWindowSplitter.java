package com.enterprise.aiassistant.backend.processing.chunking.support;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SlidingWindowSplitter {

    public static final int CHUNK_SIZE = 1000;

    public static final int OVERLAP_SIZE = 200;

    public List<String> split(String text, int chunkSize, int overlapSize) {

        List<String> pieces = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return pieces;
        }

        int start = 0;

        while (start < text.length()) {

            int end = Math.min(
                    start + chunkSize,
                    text.length()
            );

            pieces.add(text.substring(start, end));

            // Nếu đã tới cuối text thì dừng
            if (end == text.length()) {
                break;
            }

            // Lùi lại overlap
            start = end - overlapSize;
        }

        return pieces;
    }
}
