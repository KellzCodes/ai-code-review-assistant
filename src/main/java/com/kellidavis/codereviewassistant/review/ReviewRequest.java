package com.kellidavis.codereviewassistant.review;

import jakarta.validation.constraints.*;

public record ReviewRequest(
        @NotNull(message = "FilePath cannot be null")
        @NotBlank(message = "FilePath cannot be blank")
        @Size(min=1, max=500, message =  "FilePath must be between 1 and 500 characters")
        String filePath,

        @NotNull(message = "Language cannot be null")
        @NotBlank(message = "Language cannot be blank")
        @Size(min=1, max=50, message = "Language must be between 1 and 50 characters")
        String language,

        @NotNull(message = "Code cannot be null")
        @NotBlank(message = "Code cannot be blank")
        @Size(min=1, max=50000, message = "Code must be between 1 and 50,000 characters")
        String code
) {
}
