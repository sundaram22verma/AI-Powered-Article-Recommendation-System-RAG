package com.company.recsys.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Smart Tips - lightweight endpoint returning only headline and link.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SmartTipsDto {
    private String articleId;
    private String headline;
    private String seoLocation;
}