package com.aitravel.reseller.service.chat;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KnowledgeChunker {

    private static final int MAX_CHUNK_CHARACTERS = 1_000;
    private static final int OVERLAP_CHARACTERS = 150;
    private static final int MIN_BOUNDARY_DISTANCE = MAX_CHUNK_CHARACTERS / 2;

    public List<String> split(String source) {
        if (source == null || source.isBlank()) {
            return List.of();
        }

        String text = source
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int maxEnd = Math.min(start + MAX_CHUNK_CHARACTERS, text.length());
            int end = maxEnd;

            if (maxEnd < text.length()) {
                end = findBoundary(text, start, maxEnd);
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            if (end >= text.length()) {
                break;
            }

            int nextStart = findNextStart(text, start, end);
            if (nextStart <= start || nextStart >= text.length()) {
                break;
            }

            start = nextStart;
        }

        return chunks;
    }

    private int findBoundary(String text, int start, int maxEnd) {
        int minimumBoundary = start + MIN_BOUNDARY_DISTANCE;

        int paragraphBreak = text.lastIndexOf("\n\n", maxEnd - 2);
        if (paragraphBreak >= minimumBoundary) {
            return paragraphBreak + 2;
        }

        for (int index = maxEnd - 1; index >= minimumBoundary; index--) {
            char current = text.charAt(index);
            boolean sentenceEnd = ".!?。！？".indexOf(current) >= 0;
            boolean followedByWhitespace =
                    index + 1 >= text.length() || Character.isWhitespace(text.charAt(index + 1));

            if (sentenceEnd && followedByWhitespace) {
                return index + 1;
            }
        }

        for (int index = maxEnd - 1; index >= minimumBoundary; index--) {
            if (Character.isWhitespace(text.charAt(index))) {
                return index;
            }
        }

        return maxEnd;
    }

    private int findNextStart(String text, int currentStart, int end) {
        int nextStart = Math.max(currentStart + 1, end - OVERLAP_CHARACTERS);

        while (nextStart < end && !Character.isWhitespace(text.charAt(nextStart))) {
            nextStart++;
        }

        while (nextStart < text.length() && Character.isWhitespace(text.charAt(nextStart))) {
            nextStart++;
        }

        return nextStart;
    }
}