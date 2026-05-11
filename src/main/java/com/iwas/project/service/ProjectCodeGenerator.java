package com.iwas.project.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Arrays;

@Component
public class ProjectCodeGenerator {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "of", "in", "at", "on", "for", "to", "and",
            "or", "by", "with", "from", "as", "is", "are", "was", "were"
    );
    private static final int MAX_INITIALS = 7;

    public String generate(String name) {
        List<String> words = Arrays.stream(name.trim().split("\\s+"))
                .map(w -> w.replaceAll("[^A-Za-z0-9]", ""))
                .filter(w -> !w.isEmpty())
                .filter(w -> !STOP_WORDS.contains(w.toLowerCase()))
                .collect(Collectors.toList());

        if (words.isEmpty()) {
            String stripped = name.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
            return stripped.length() >= 2
                    ? stripped.substring(0, Math.min(5, stripped.length()))
                    : "PRJ";
        }

        String base;
        if (words.size() >= 3) {
            base = words.stream()
                    .map(w -> String.valueOf(w.charAt(0)))
                    .collect(Collectors.joining());
            base = base.substring(0, Math.min(MAX_INITIALS, base.length()));
        } else if (words.size() == 2) {
            base = "" + words.get(0).charAt(0) + words.get(1).charAt(0);
        } else {
            String word = words.get(0).toUpperCase();
            base = word.substring(0, Math.min(3, word.length()));
        }

        return base.toUpperCase();
    }
}
