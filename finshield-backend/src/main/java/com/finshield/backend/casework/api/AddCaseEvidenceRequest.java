package com.finshield.backend.casework.api;

import com.finshield.backend.casework.domain.EvidenceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddCaseEvidenceRequest(
        @NotNull EvidenceType evidenceType,
        @NotBlank @Size(max = 255) String fileName,
        @NotBlank @Size(max = 1000)
        @Pattern(regexp = "^(https://|s3://|gs://|azure://).+", message = "fileUrl must be an approved remote object URI")
        String fileUrl,
        @Size(max = 2000) String description
) {}
