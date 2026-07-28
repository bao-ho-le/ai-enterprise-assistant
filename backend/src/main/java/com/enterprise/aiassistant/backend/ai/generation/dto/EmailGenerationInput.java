package com.enterprise.aiassistant.backend.ai.generation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EmailGenerationInput {

    private String recipient;

    @NotBlank
    @Size(max = 2000)
    private String purpose;

    private String tone;

    // Short | Medium | Long
    private String length;

    private String language;

    @Size(max = 200)
    private String senderName;
}
