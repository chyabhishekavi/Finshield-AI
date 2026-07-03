package com.finshield.backend.casework.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddCaseNoteRequest(
        @NotBlank @Size(max = 4000) String noteText,
        @NotNull Boolean internalOnly
) {}
