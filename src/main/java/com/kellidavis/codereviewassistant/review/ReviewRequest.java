package com.kellidavis.codereviewassistant.review;

import jakarta.validation.constraints.*;

public record ReviewRequest(
        @NotNull(message = "FilePath cannot be null")
        @NotBlank(message = "FilePath cannot be blank")
        @Size(max=500, message =  "FilePath cannot be more than 500 characters")
        String filePath,

        @NotNull(message = "Language cannot be null")
        @NotBlank(message = "Language cannot be blank")
        @Size(max=50, message = "Language cannot be more than 50 characters")
        String language,

        @NotNull(message = "Code cannot be null")
        @NotBlank(message = "Code cannot be blank")
        @Size(max=50000, message = "Code cannot be more than 50,000 characters")
        String code
) {
}
