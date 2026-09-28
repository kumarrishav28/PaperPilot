package com.PaperPilot.service;

import com.PaperPilot.config.PaperPilotProperties;
import com.PaperPilot.dto.CitationDto;
import com.PaperPilot.dto.ResponseDto;
import com.PaperPilot.dto.SearchResultDTO;
import com.PaperPilot.dto.UserInputDto;
import com.PaperPilot.exception.AiServiceException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.InterruptedIOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RetrievalAugmentedGenerationService {

    private final Logger logger = LoggerFactory.getLogger(RetrievalAugmentedGenerationService.class);

    private final PaperPilotProperties appProperties;

    private final VectorStore vectorStore;

    private final ChatClient chatClient;

    /**
     * Generate a response based on the user input by retrieving relevant information from the vector store and using it to prompt the LLM.
     *
     * @param userInput The user input containing the query, documentId, topK, and similarityThreshold.
     * @return A ResponseDto containing the generated response, citations, and response time.
     */
    public ResponseDto generateResponse(UserInputDto userInput) {
        long startTime = System.currentTimeMillis();
        logger.info("processing user Query :{} for documentId: {}", userInput.getQuery(), userInput.getDocumentId());

        // step 1: Retrieve relevant information from the vector store
        List<Document> retrievedDocuments = retrieveRelevantInformation(userInput);

        // step 2 : Map Citation and References from the retrieved documents
        List<CitationDto> citations = retrievedDocuments.stream()
                .map(this::mapCitations)
                .toList();

        // step 3 : Build context for the LLM from the retrieved documents
        String rawContext = buildContextFromRetrievedDocuments(retrievedDocuments);
        String context = truncateContext(rawContext, appProperties.getRag().getMaxContextChars());
        String groundedPrompt = buildGroundedUserPrompt(userInput.getQuery(), context);

        //step 4 : Generate response from the LLM using the context and user query
        String llmResponse = callWithTimeoutFallback(userInput.getQuery(), groundedPrompt, rawContext);

        logger.info("LLM response generated for query: {} in {} ms", userInput.getQuery(), System.currentTimeMillis() - startTime);
        return ResponseDto.builder()
                .response(llmResponse)
                .citations(citations)
                .responseTime(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * Generate a response stream based on the user input by retrieving relevant information from the vector store and using it to prompt the LLM.
     *
     * @param userInput The user input containing the query, documentId, topK, and similarityThreshold.
     * @return A Flux<String> containing the generated response stream.
     */
    public Flux<String> generateResponseStream(UserInputDto userInput) {
        long startTime = System.currentTimeMillis();
        logger.info("generateResponseStream -- processing user Query :{} for documentId: {}", userInput.getQuery(), userInput.getDocumentId());

        // step 1: Retrieve relevant information from the vector store
        List<Document> retrievedDocuments = retrieveRelevantInformation(userInput);

        // step 2 : Map Citation and References from the retrieved documents
        List<CitationDto> citations = retrievedDocuments.stream()
                .map(this::mapCitations)
                .toList();

        // step 3 : Build context for the LLM from the retrieved documents
        String rawContext = buildContextFromRetrievedDocuments(retrievedDocuments);
        String context = truncateContext(rawContext, appProperties.getRag().getMaxContextChars());
        String groundedPrompt = buildGroundedUserPrompt(userInput.getQuery(), context);

        //step 4 : Generate response from the LLM using the context and user query
        return this.chatClient.prompt()
                .user(groundedPrompt)
                .stream()
                .content()
                .concatWith(Flux.just("\n"))
                .onErrorResume(ex -> {
                    if (!isTimeoutLike(ex)) {
                        return Flux.error(new AiServiceException("AI provider failed to stream response. Please retry.",
                                asException(ex), false));
                    }
                    logger.warn("Streaming timed out. Retrying with compact context.");
                    try {
                        String compact = truncateContext(rawContext, appProperties.getRag().getRetryContextChars());
                        String retryPrompt = buildGroundedUserPrompt(userInput.getQuery(), compact);
                        String fallback = chatCall(retryPrompt);
                        return Flux.just(fallback).concatWith(Flux.just("\n"));
                    } catch (Exception retryEx) {
                        return Flux.error(new AiServiceException(
                                "AI provider timeout while generating response. Please retry in a moment.",
                                asException(retryEx),
                                true
                        ));
                    }
                })
                .doOnTerminate(() -> logger.info("LLM response stream generated for query: {} in {} ms", userInput.getQuery(), System.currentTimeMillis() - startTime));
    }

    private String callWithTimeoutFallback(String query, String prompt, String rawContext) {
        try {
            return chatCall(prompt);
        } catch (Exception ex) {
            if (!isTimeoutLike(ex)) {
                throw new AiServiceException("AI provider failed to generate response. Please retry.", asException(ex), false);
            }

            logger.warn("Timeout while generating response for query: {}. Retrying with compact context.", query);
            try {
                String compactContext = truncateContext(rawContext, appProperties.getRag().getRetryContextChars());
                String retryPrompt = buildGroundedUserPrompt(query, compactContext);
                return chatCall(retryPrompt);
            } catch (Exception retryEx) {
                throw new AiServiceException("AI provider timeout while generating response. Please retry in a moment.",
                        asException(retryEx), true);
            }
        }
    }

    private String chatCall(String prompt) {
        return this.chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

    private String buildGroundedUserPrompt(String query, String context) {
        if (context == null || context.isBlank()) {
            return """
                    No relevant document context was retrieved for this question.
                    Explain this clearly and ask the user to choose/upload an indexed document before answering with assumptions.

                    User question:
                    """
                    + query;
        }

        return """
                You are answering a question using retrieved document context.
                Use the context below as the primary source of truth and cite concrete details from it.
                If the context is insufficient, say exactly what is missing.

                Retrieved context:
                """
                + context
                + """

                User question:
                """
                + query;
    }

    /**
     * Build context for the LLM from the retrieved documents.
     *
     * @param similarRetrievedDocuments The list of documents retrieved from the vector store.
     * @return A string containing the context built from the retrieved documents.
     */
    private String buildContextFromRetrievedDocuments(List<Document> similarRetrievedDocuments) {
        if (similarRetrievedDocuments == null || similarRetrievedDocuments.isEmpty()) {
            return "";
        }
        return similarRetrievedDocuments.stream().map(document -> {
            String filename = (String) document.getMetadata().getOrDefault("filename",
                    document.getMetadata().getOrDefault("fileName", "Unknown"));
            Object pageNumberObj = document.getMetadata().get("pageNumber");
            return String.format("[Source: %s | Page: %s]\n%s", filename, pageNumberObj != null ? pageNumberObj.toString() : "Unknown", document.getText());
        }).collect(Collectors.joining("\n\n---\n\n"));

    }

    /**
     * Map the retrieved documents to CitationDto objects.
     *
     * @param retrievedDocument The document retrieved from the vector store.
     * @return A CitationDto object containing relevant information from the retrieved document.
     */
    private CitationDto mapCitations(Document retrievedDocument) {
        Map<String, Object> additionalInfo = retrievedDocument.getMetadata();
        UUID documentId = mapDocumentId(additionalInfo.get("documentId"));
        if (documentId != null) {
            additionalInfo.put("documentId", documentId.toString());
        }

        Integer chunkIndex = null;
        if (additionalInfo.get("chunkIndex") instanceof Number n) {
            chunkIndex = n.intValue();
        }

        Integer pageNumber = null;
        if (additionalInfo.get("pageNumber") instanceof Number n) {
            pageNumber = n.intValue();
        } else if (additionalInfo.get("page_number") instanceof Number n) {
            pageNumber = n.intValue();
        }

        Double score = null;
        if (additionalInfo.get("distance") instanceof Number n) {
            score = 1.0 - n.doubleValue();
        }

        return CitationDto.builder()
                .documentId(documentId)
                .filename((String) additionalInfo.getOrDefault("filename",
                        additionalInfo.getOrDefault("fileName", "Unknown")))
                .chunkIndex(chunkIndex)
                .pageNumber(pageNumber)
                .snippet(retrievedDocument.getText())
                .similarityScore(score)
                .additionalProperties(additionalInfo)
                .build();
    }

    private UUID mapDocumentId(Object rawDocumentId) {
        if (rawDocumentId instanceof UUID uuid) {
            return uuid;
        }
        if (rawDocumentId instanceof String value && !value.isBlank()) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException ignored) {
                return null;
            }
        }
        return null;
    }

    /**
     * Retrieve relevant information from the vector store based on the user input.
     *
     * @param userInput The user input containing the query, documentId, topK, and similarityThreshold.
     * @return A list of relevant documents retrieved from the vector store.
     */
    private List<Document> retrieveRelevantInformation(UserInputDto userInput) {

        int topK = (userInput.getTopK() != null && userInput.getTopK() > 0)
                ? userInput.getTopK()
                : appProperties.getRag().getTopK();

        double similarityThreshold = (userInput.getSimilarityThreshold() != null && userInput.getSimilarityThreshold() >= 0.0)
                ? userInput.getSimilarityThreshold()
                : appProperties.getRag().getSimilarityThreshold();

        // STEP 1 : BUILD SEARCH REQUEST

        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder()
                .query(userInput.getQuery())
                .topK(topK)
                .similarityThreshold(similarityThreshold);

        // Step 2 : Filter expression to filter by documentId if provided
        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();
        Filter.Expression filterExpression = null;
        if (userInput.getDocumentId() != null) {
            logger.info("Filtering by documentId: {}", userInput.getDocumentId());
            filterExpression = filterExpressionBuilder.eq("documentId", userInput.getDocumentId().toString()).build();
        }
        if (filterExpression != null) {
            searchRequestBuilder.filterExpression(filterExpression);
        }

        // Step 3 : Execute the search request

        try {
            List<Document> retrievedDocuments = vectorStore.similaritySearch(searchRequestBuilder.build());

            if (retrievedDocuments.isEmpty() && userInput.getDocumentId() != null) {
                logger.warn("No documents found with direct metadata filter. Retrying with broader search and in-memory documentId match.");
                List<Document> unfilteredDocs = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(userInput.getQuery())
                                .topK(topK)
                                .similarityThreshold(similarityThreshold)
                                .build()
                );
                String expectedDocumentId = userInput.getDocumentId().toString();
                retrievedDocuments = unfilteredDocs.stream()
                        .filter(doc -> metadataMatchesDocumentId(doc.getMetadata(), expectedDocumentId))
                        .toList();
            }

            if (retrievedDocuments.isEmpty() && similarityThreshold > appProperties.getRag().getSimilarityThreshold()) {
                logger.warn("No documents found with similarity threshold {}. Retrying with default threshold {}.",
                        similarityThreshold, appProperties.getRag().getSimilarityThreshold());
                SearchRequest.Builder fallbackBuilder = SearchRequest.builder()
                        .query(userInput.getQuery())
                        .topK(topK)
                        .similarityThreshold(appProperties.getRag().getSimilarityThreshold());

                if (filterExpression != null) {
                    fallbackBuilder.filterExpression(filterExpression);
                }

                retrievedDocuments = vectorStore.similaritySearch(fallbackBuilder.build());
            }

            logger.info("Retrieved {} documents for query: {}", retrievedDocuments.size(), userInput.getQuery());
            return retrievedDocuments;
        } catch (Exception e) {
            logger.error("Error retrieving documents for query: {}. Error: {}", userInput.getQuery(), e.getMessage());
            throw new RuntimeException("Error retrieving documents", e);
        }
    }

    private String truncateContext(String context, int maxChars) {
        if (context == null || context.isBlank() || maxChars <= 0 || context.length() <= maxChars) {
            return context;
        }
        return context.substring(0, maxChars)
                + "\n\n[Context truncated to stay within model/provider limits.]";
    }

    private boolean isTimeoutLike(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof InterruptedIOException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && message.toLowerCase().contains("timeout")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private Exception asException(Throwable throwable) {
        if (throwable instanceof Exception exception) {
            return exception;
        }
        return new RuntimeException(throwable);
    }

    private boolean metadataMatchesDocumentId(Map<String, Object> metadata, String expectedDocumentId) {
        if (metadata == null || expectedDocumentId == null || expectedDocumentId.isBlank()) {
            return false;
        }
        Object raw = metadata.get("documentId");
        if (raw == null) {
            return false;
        }
        if (raw instanceof UUID uuid) {
            return expectedDocumentId.equals(uuid.toString());
        }
        return expectedDocumentId.equals(raw.toString());
    }

    /**
     * Search for similar documents based on the user input by retrieving relevant information
     * from the vector store and mapping it to a SearchResultDTO.
     *
     * @param userInput The user input containing the query, documentId, topK, and similarityThreshold.
     * @return A SearchResultDTO containing the query, total matches, and citations of the retrieved documents.
     */
    public SearchResultDTO searchSimilarDocuments(UserInputDto userInput) {
        List<Document> retrievedDocuments = retrieveRelevantInformation(userInput);

        List<CitationDto> citations = retrievedDocuments.stream()
                .map(this::mapCitations)
                .toList();

        return SearchResultDTO.builder()
                .query(userInput.getQuery())
                .totalMatches(citations.size())
                .citations(citations)
                .build();
    }
}
