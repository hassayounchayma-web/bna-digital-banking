package com.bna.digitalbanking.credit.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationNoteRequest(@NotBlank @Size(max = 1000) String note) {
}
