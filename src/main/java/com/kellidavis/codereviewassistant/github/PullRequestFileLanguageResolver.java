package com.kellidavis.codereviewassistant.github;

import org.springframework.stereotype.Component;
import java.util.Locale;
import java.util.Map;

@Component
public class PullRequestFileLanguageResolver {
    private static final String UNKNOWN_LANGUAGE = "Unknown";

    private static final Map<String, String> LANGUAGE_BY_EXTENSION = Map.ofEntries(
            Map.entry("java", "Java"),
            Map.entry("kt", "Kotlin"),
            Map.entry("kts", "Kotlin"),
            Map.entry("js", "JavaScript"),
            Map.entry("jsx", "JavaScript"),
            Map.entry("ts", "TypeScript"),
            Map.entry("tsx", "TypeScript"),
            Map.entry("py", "Python"),
            Map.entry("go", "Go"),
            Map.entry("rb", "Ruby"),
            Map.entry("cs", "C#"),
            Map.entry("c", "C"),
            Map.entry("h", "C"),
            Map.entry("cc", "C++"),
            Map.entry("cpp", "C++"),
            Map.entry("cxx", "C++"),
            Map.entry("hpp", "C++"),
            Map.entry("hxx", "C++"),
            Map.entry("rs", "Rust"),
            Map.entry("sql", "SQL"),
            Map.entry("json", "JSON"),
            Map.entry("xml", "XML"),
            Map.entry("yml", "YAML"),
            Map.entry("yaml", "YAML"),
            Map.entry("md", "Markdown"));

    public String resolveLanguage(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return UNKNOWN_LANGUAGE;
        }

        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == filePath.length() - 1) {
            return UNKNOWN_LANGUAGE;
        }

        String extension = filePath.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
        return LANGUAGE_BY_EXTENSION.getOrDefault(extension, UNKNOWN_LANGUAGE);
    }
}
