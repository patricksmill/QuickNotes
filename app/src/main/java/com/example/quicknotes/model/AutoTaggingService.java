package com.example.quicknotes.model;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputMessage;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * AutoTaggingService handles automatic tag assignment for notes.
 * It supports both keyword-based and AI-powered tagging strategies.
 */
public class AutoTaggingService {
    private final Context ctx;
    private final TagOperationsManager tagOperations;

    /**
     * Functional interface for tag assignment callback.
     */
    public interface TagAssignmentCallback {
        void onTagAssigned(String tagName);
    }

    /**
     * Constructs an AutoTaggingService instance.
     *
     * @param ctx The application context
     * @param tagOperations The tag operations manager for tag assignment
     */
    public AutoTaggingService(@NonNull Context ctx, @NonNull TagOperationsManager tagOperations) {
        this.ctx = ctx.getApplicationContext();
        this.tagOperations = tagOperations;
    }

    /**
     * Assigns tags to a note based on keyword matching, up to the specified limit.
     *
     * @param note  The note to auto-tag
     * @param limit The maximum number of tags to assign
     */
    public void performSimpleAutoTag(@NonNull Note note, int limit) {
        if (limit <= 0) return;

        String combined = extractTextContent(note);
        Set<String> words = extractWords(combined);
        Map<String, String> dictionary = KeywordTagDictionary.loadTagMap(ctx);

        assignTagsFromDictionary(note, words, dictionary, limit);
    }

    /**
     * Uses AI to suggest and assign tags to a note, up to the specified limit.
     *
     * @param note  The note to auto-tag
     * @param limit The maximum number of tags to assign
     * @param apiKey The OpenAI API key
     * @param existingTags Set of existing tags to consider
     * @param callback Callback for individual tag assignments
     */
    public void performAiAutoTag(@NonNull Note note, int limit, @NonNull String apiKey, 
                                @NonNull Set<String> existingTags, @NonNull TagAssignmentCallback callback) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler uiHandler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                String tagCsv = requestAiTags(note, limit, apiKey, existingTags);
                processAiTagResponse(tagCsv, callback, uiHandler);
            } catch (Exception e) {
                handleAiTaggingError(e, uiHandler);
            } finally {
                executor.shutdown();
            }
        });
    }

    /**
     * Extracts text content from note title and content.
     *
     * @param note The note to extract text from
     * @return Combined text content in lowercase
     */
    private String extractTextContent(@NonNull Note note) {
        return (note.getTitle() + " " + note.getContent()).toLowerCase();
    }

    /**
     * Extracts individual words from text content.
     *
     * @param text The text to extract words from
     * @return Set of unique words
     */
    private Set<String> extractWords(@NonNull String text) {
        return Arrays.stream(text.split("\\W+"))
                .filter(w -> !w.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Assigns tags based on keyword dictionary matching.
     *
     * @param note The note to tag
     * @param words Set of words from the note
     * @param dictionary Keyword to tag mapping
     * @param limit Maximum number of tags to assign
     */
    private void assignTagsFromDictionary(@NonNull Note note, @NonNull Set<String> words, 
                                        @NonNull Map<String, String> dictionary, int limit) {
        int count = 0;
        for (String word : words) {
            String tagName = dictionary.get(word);
            if (tagName != null && !hasTag(note, tagName)) {
                tagOperations.setTag(note, tagName);
                count++;
                if (count >= limit) {
                    break;
                }
            }
        }
    }

    /**
     * Checks if a note already has a specific tag.
     *
     * @param note The note to check
     * @param tagName The tag name to look for
     * @return true if the note has the tag, false otherwise
     */
    private boolean hasTag(@NonNull Note note, @NonNull String tagName) {
        return note.getTags().stream()
                .anyMatch(t -> t.getName().equalsIgnoreCase(tagName));
    }

    /**
     * Requests AI-generated tags from OpenAI API.
     *
     * @param note The note to analyze
     * @param limit Maximum number of tags to request
     * @param apiKey OpenAI API key
     * @param existingTags Existing tags to consider
     * @return Comma-separated list of suggested tags
     */
    private String requestAiTags(@NonNull Note note, int limit, @NonNull String apiKey, 
                                @NonNull Set<String> existingTags) {
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();

        String prompt = buildAiPrompt(note, limit, existingTags);
        
        ResponseCreateParams params = ResponseCreateParams.builder()
                .input(prompt)
                .model(ChatModel.GPT_4_1_NANO)
                .build();

        Response response = client.responses().create(params);
        return extractResponseText(response);
    }

    /**
     * Builds the prompt for AI tag generation.
     *
     * @param note The note to analyze
     * @param limit Maximum number of tags
     * @param existingTags Existing tags to consider
     * @return Formatted prompt string
     */
    private String buildAiPrompt(@NonNull Note note, int limit, @NonNull Set<String> existingTags) {
        return "system: You are a tag suggestion assistant that outputs a comma-separated list of tag names.\n" +
                "Use existing tags when appropriate. Do not explain or output anything other than the tag list.\n" +
                "Existing tags: " + String.join(", ", existingTags) + "\n" +
                "user: Extract up to " + limit + " tags from the following text:\n" +
                "Title: " + note.getTitle() + "\n" +
                "Content: " + note.getContent();
    }

    /**
     * Extracts text from OpenAI API response.
     *
     * @param response The API response
     * @return Extracted text content
     */
    private String extractResponseText(@NonNull Response response) {
        StringBuilder sb = new StringBuilder();
        for (ResponseOutputItem item : response.output()) {
            if (!item.isMessage()) continue;
            
            ResponseOutputMessage msg = item.asMessage();
            for (ResponseOutputMessage.Content content : msg.content()) {
                if (content.isOutputText()) {
                    sb.append(content.asOutputText().text());
                }
            }
            break;
        }
        return sb.toString().trim();
    }

    /**
     * Processes AI tag response and assigns tags.
     *
     * @param tagCsv Comma-separated tag string
     * @param callback Callback for tag assignment
     * @param uiHandler Handler for UI thread operations
     */
    private void processAiTagResponse(@NonNull String tagCsv, @NonNull TagAssignmentCallback callback, 
                                    @NonNull Handler uiHandler) {
        if (tagCsv.isEmpty()) return;

        String[] tagNames = tagCsv.split("\\s*,\\s*");
        for (String rawTag : tagNames) {
            String tag = rawTag.trim();
            if (!tag.isEmpty()) {
                uiHandler.post(() -> callback.onTagAssigned(tag));
            }
        }
    }

    /**
     * Handles errors during AI tagging.
     *
     * @param e The exception that occurred
     * @param uiHandler Handler for UI thread operations
     */
    private void handleAiTaggingError(@NonNull Exception e, @NonNull Handler uiHandler) {
        e.printStackTrace();
        uiHandler.post(() ->
                Toast.makeText(ctx, "Auto-tag error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show()
        );
    }
} 