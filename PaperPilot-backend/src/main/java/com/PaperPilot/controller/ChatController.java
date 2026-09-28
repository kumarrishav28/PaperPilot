package com.PaperPilot.controller;

import com.PaperPilot.dto.ApiFromResponse;
import com.PaperPilot.dto.ResponseDto;
import com.PaperPilot.dto.SearchResultDTO;
import com.PaperPilot.dto.UserInputDto;
import com.PaperPilot.service.RetrievalAugmentedGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat Management", description = "APIs for managing chat interactions")
@RequiredArgsConstructor
public class ChatController {

    private final RetrievalAugmentedGenerationService retrievalAugmentedGenerationService;

    @PostMapping("/generate-response")
    @Operation(summary = "Generate Response", description = "Generates a response based on user input and retrieves relevant information from the vector store.")
    public ResponseEntity<ApiFromResponse<ResponseDto>> chatAndGenerateResponse ( @RequestBody UserInputDto userInputDto) {

        ResponseDto response = retrievalAugmentedGenerationService.generateResponse(userInputDto);

        ApiFromResponse<ResponseDto> apiResponse = ApiFromResponse.<ResponseDto>builder()
                .success(true)
                .message("Response generated successfully")
                .data(response)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/stream-generate-response")
    @Operation(summary = "Stream Generate Response", description = "Generates a response stream based on user input and retrieves relevant information from the vector store.")
    public Flux<String> streamChatAndGenerateResponse(@RequestBody UserInputDto userInputDto) {
        return retrievalAugmentedGenerationService.generateResponseStream(userInputDto);
    }


    @PostMapping("/search-similar")
    @Operation(summary = "Search Similar Documents", description = "Searches for similar documents based on user input and retrieves relevant information from the vector store.")
    public ResponseEntity<ApiFromResponse<SearchResultDTO>> searchSimilar ( @RequestBody UserInputDto userInputDto) {

        SearchResultDTO response = retrievalAugmentedGenerationService.searchSimilarDocuments(userInputDto);

        ApiFromResponse<SearchResultDTO> apiResponse = ApiFromResponse.<SearchResultDTO>builder()
                .success(true)
                .message("Response generated successfully")
                .data(response)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}
