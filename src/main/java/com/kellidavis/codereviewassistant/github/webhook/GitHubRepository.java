package com.kellidavis.codereviewassistant.github.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepository(
      @NotBlank(message = "Repository name is required")
      @JsonProperty("full_name")
      String fullName
) {
}
