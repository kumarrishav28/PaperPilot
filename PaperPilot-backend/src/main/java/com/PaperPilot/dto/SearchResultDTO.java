package com.PaperPilot.dto;

import lombok.*;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SearchResultDTO {
    private String query;
    private int totalMatches;
    private List<CitationDto> citations;
}
