package com.company.recsys.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for search recommendation results.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchResultDto {
    private String articleId;
    private String headline;
    private String seoLocation;
    private String synopsis;
    private Double similarityScore;
}